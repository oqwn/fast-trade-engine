package com.fasttrader.controller;

import com.fasttrader.controller.dto.AccountRequest;
import com.fasttrader.controller.dto.AccountResponse;
import com.fasttrader.controller.dto.DepositWithdrawRequest;
import com.fasttrader.controller.dto.PositionResponse;
import com.fasttrader.model.Account;
import com.fasttrader.model.Position;
import com.fasttrader.service.AccountService;
import com.fasttrader.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    private final MarketDataService marketDataService;
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        log.info("Creating account: {}", request);
        
        Account account = accountService.createAccount(
            request.getAccountId(), 
            request.getAccountName(), 
            request.getInitialBalance());
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(AccountResponse.from(account));
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return accountService.getAccount(accountId)
            .map(AccountResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable String accountId) {
        return accountService.getAccount(accountId)
            .map(account -> Map.of(
                "balance", account.getBalance(),
                "availableBalance", account.getAvailableBalance(),
                "frozenBalance", account.getFrozenBalance()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{accountId}/positions")
    public ResponseEntity<List<PositionResponse>> getPositions(@PathVariable String accountId) {
        Map<String, Position> positions = accountService.getPositions(accountId);
        Map<String, BigDecimal> currentPrices = marketDataService.getCurrentPrices();
        
        List<PositionResponse> response = positions.values().stream()
            .map(position -> {
                BigDecimal currentPrice = currentPrices.get(position.getSymbol());
                return PositionResponse.from(position, currentPrice);
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Map<String, String>> deposit(
            @PathVariable String accountId,
            @Valid @RequestBody DepositWithdrawRequest request) {
        
        log.info("Depositing {} to account {}", request.getAmount(), accountId);
        
        accountService.deposit(accountId, request.getAmount());
        
        return ResponseEntity.ok(Map.of(
            "message", "Deposit successful",
            "accountId", accountId,
            "amount", request.getAmount().toString()
        ));
    }
    
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody DepositWithdrawRequest request) {
        
        log.info("Withdrawing {} from account {}", request.getAmount(), accountId);
        
        accountService.withdraw(accountId, request.getAmount());
        
        return ResponseEntity.ok(Map.of(
            "message", "Withdrawal successful",
            "accountId", accountId,
            "amount", request.getAmount().toString()
        ));
    }
    
    @GetMapping("/{accountId}/equity")
    public ResponseEntity<Map<String, BigDecimal>> getTotalEquity(@PathVariable String accountId) {
        Map<String, BigDecimal> currentPrices = marketDataService.getCurrentPrices();
        BigDecimal totalEquity = accountService.getTotalEquity(accountId, currentPrices);
        
        return ResponseEntity.ok(Map.of(
            "totalEquity", totalEquity,
            "timestamp", BigDecimal.valueOf(System.currentTimeMillis())
        ));
    }
}