package com.example.account.dto;

import java.time.LocalDateTime;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class QueryTransaction {
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class QtResponse{
        private String accountNumber;
        private TransactionType transactionType;
        private TransactionResultType transactionResult;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        public static QtResponse from(TransactionDto transactionDto) {
            return QtResponse.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionType(transactionDto.getTransactionType())
                    .transactionResult(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .transactedAt(transactionDto.getTransactedAt())
                    .build();
        }
    }
}