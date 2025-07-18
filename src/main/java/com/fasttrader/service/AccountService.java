package com.fasttrader.service;

import com.fasttrader.model.Account;
import com.fasttrader.model.Order;
import com.fasttrader.model.Position;
import com.fasttrader.model.Trade;
import com.fasttrader.model.enums.OrderSide;
import com.fasttrader.model.enums.OrderType;
import com.fasttrader.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final Map<String, BigDecimal> frozenAmounts = new ConcurrentHashMap<>();
    
    @Transactional
    public Account createAccount(String accountId, String accountName, BigDecimal initialBalance) {
        if (accountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
        
        Account account = Account.builder()
            .accountId(accountId)
            .accountName(accountName)
            .balance(initialBalance)
            .availableBalance(initialBalance)
            .frozenBalance(BigDecimal.ZERO)
            .build();
        
        return accountRepository.save(account);
    }
    
    public Optional<Account> getAccount(String accountId) {
        return accountRepository.findById(accountId);
    }
    
    @Transactional
    public void validateAndFreezeForOrder(Order order) {
        Account account = accountRepository.findById(order.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + order.getAccountId()));
        
        BigDecimal requiredAmount = calculateRequiredAmount(order);
        
        if (!account.hasAvailableBalance(requiredAmount)) {
            throw new IllegalStateException("Insufficient balance. Required: " + 
                requiredAmount + ", Available: " + account.getAvailableBalance());
        }
        
        account.freezeFunds(requiredAmount);
        accountRepository.save(account);
        
        frozenAmounts.put(order.getOrderId(), requiredAmount);
        
        log.debug("Frozen {} for order {}", requiredAmount, order.getOrderId());
    }
    
    @Transactional
    public void unfreezeForOrder(Order order) {
        BigDecimal frozenAmount = frozenAmounts.remove(order.getOrderId());
        if (frozenAmount == null) {
            return;
        }
        
        Account account = accountRepository.findById(order.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + order.getAccountId()));
        
        account.unfreezeFunds(frozenAmount);
        accountRepository.save(account);
        
        log.debug("Unfrozen {} for order {}", frozenAmount, order.getOrderId());
    }
    
    @Transactional
    public void unfreezeRemainingFunds(Order order) {
        BigDecimal frozenAmount = frozenAmounts.get(order.getOrderId());
        if (frozenAmount == null) {
            return;
        }
        
        BigDecimal usedAmount = calculateUsedAmount(order);
        BigDecimal remainingFrozen = frozenAmount.subtract(usedAmount);
        
        if (remainingFrozen.compareTo(BigDecimal.ZERO) > 0) {
            Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + order.getAccountId()));
            
            account.unfreezeFunds(remainingFrozen);
            accountRepository.save(account);
            
            log.debug("Unfrozen remaining {} for order {}", remainingFrozen, order.getOrderId());
        }
        
        frozenAmounts.remove(order.getOrderId());
    }
    
    @Transactional
    public void processTrade(Trade trade) {
        Account buyAccount = accountRepository.findById(trade.getBuyAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Buy account not found: " + trade.getBuyAccountId()));
        
        Account sellAccount = accountRepository.findById(trade.getSellAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Sell account not found: " + trade.getSellAccountId()));
        
        BigDecimal tradeValue = trade.getValue();
        
        buyAccount.deductFunds(tradeValue);
        buyAccount.updatePosition(trade.getSymbol(), trade.getQuantity(), trade.getPrice(), true);
        
        sellAccount.addFunds(tradeValue);
        sellAccount.updatePosition(trade.getSymbol(), trade.getQuantity(), trade.getPrice(), false);
        
        accountRepository.save(buyAccount);
        accountRepository.save(sellAccount);
        
        log.info("Processed trade {} between accounts {} and {}", 
            trade.getTradeId(), trade.getBuyAccountId(), trade.getSellAccountId());
    }
    
    public BigDecimal getTotalEquity(String accountId, Map<String, BigDecimal> marketPrices) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        return account.getTotalEquity(marketPrices);
    }
    
    public Map<String, Position> getPositions(String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        return account.getPositions();
    }
    
    @Transactional
    public void deposit(String accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        account.addFunds(amount);
        accountRepository.save(account);
        
        log.info("Deposited {} to account {}", amount, accountId);
    }
    
    @Transactional
    public void withdraw(String accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        if (!account.hasAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance for withdrawal");
        }
        
        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepository.save(account);
        
        log.info("Withdrew {} from account {}", amount, accountId);
    }
    
    private BigDecimal calculateRequiredAmount(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            if (order.getType() == OrderType.MARKET) {
                return order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()))
                    .multiply(BigDecimal.valueOf(1.1));
            } else {
                return order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            }
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateUsedAmount(Order order) {
        if (order.getSide() == OrderSide.BUY && order.getFilledQuantity() > 0) {
            return order.getPrice().multiply(BigDecimal.valueOf(order.getFilledQuantity()));
        }
        return BigDecimal.ZERO;
    }
}