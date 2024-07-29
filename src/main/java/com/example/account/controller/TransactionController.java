package com.example.account.controller;

import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;

import lombok.RequiredArgsConstructor;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request
    ) throws InterruptedException {
        try {
            Thread.sleep(5000L);
            return UseBalance.Response.from(
                    transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance. ");

            transactionService.saveAndFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @DeleteMapping("/cancel")
    @AccountLock
    public CancelBalance.CbResponse cancelBalance(
            @Valid @RequestBody CancelBalance.CbRequest request
    ) throws InterruptedException {
        try {
            return CancelBalance.CbResponse.fromDto(
                    transactionService.cancelBalance(
                            request.getTransactionId(),
                            request.getAccountNumber(),
                            request.getAmount())
            );
        } catch(AccountException e) {
            transactionService.saveAndFailedUseTransaction(
                    TransactionType.CANCEL,
                    TransactionResultType.F,
                    request.getAccountNumber(),
                    request.getAmount());
            throw e;
        }
    }
}