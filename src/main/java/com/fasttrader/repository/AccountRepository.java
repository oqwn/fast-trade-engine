package com.fasttrader.repository;

import com.fasttrader.model.Account;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class AccountRepository {
    
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    
    public Account save(Account account) {
        accounts.put(account.getAccountId(), account);
        return account;
    }
    
    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }
    
    public List<Account> findAll() {
        return accounts.values().stream()
            .sorted((a1, a2) -> a1.getAccountId().compareTo(a2.getAccountId()))
            .collect(Collectors.toList());
    }
    
    public List<Account> findByActive(boolean active) {
        return accounts.values().stream()
            .filter(account -> account.isActive() == active)
            .collect(Collectors.toList());
    }
    
    public void deleteById(String accountId) {
        accounts.remove(accountId);
    }
    
    public void deleteAll() {
        accounts.clear();
    }
    
    public long count() {
        return accounts.size();
    }
    
    public boolean existsById(String accountId) {
        return accounts.containsKey(accountId);
    }
}