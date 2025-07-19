package com.fasttrader.controller.dto;

import com.fasttrader.model.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    
    private String accountId;
    private String accountName;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    
    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
            .accountId(account.getAccountId())
            .accountName(account.getAccountName())
            .balance(account.getBalance())
            .availableBalance(account.getAvailableBalance())
            .frozenBalance(account.getFrozenBalance())
            .active(account.isActive())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .build();
    }
}