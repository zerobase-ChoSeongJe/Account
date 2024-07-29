package com.example.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;

@ExtendWith(MockitoExtension.class)
@DisplayName("거래 관련 서비스 테스트")
class TransactionServiceTest {
    private final long minAmount = 9L;
    private final long maxAmount = 100_000_001L;
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 - 잔액 사용 성공")
    void useBalance_sucess() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10_000L)
                .accountNumber("1000000000")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .account(account)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build())
        ;

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.useBalance(1L, "1000000000", 900L);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(TransactionType.USE, captor.getValue().getTransactionType());
        assertEquals(TransactionResultType.S, captor.getValue().getTransactionResultType());
        assertEquals(900L, captor.getValue().getAmount());
        assertEquals(9_100L, captor.getValue().getBalanceSnapshot());
    }

    @Test
    @DisplayName("잔액 사용 - 해당 유저 없음")
    void useBalance_userNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty())
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1_000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 해당 계좌 없음")
    void useBalance_accountNotFound() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty())
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "10000000000", 1000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 계좌 소유주 다름")
    void useBalance_userAccountUnMatch() {
        // given
        AccountUser accountKim = AccountUser.builder()
                .name("Kim")
                .build();
        accountKim.setId(1L);

        AccountUser accountLee = AccountUser.builder()
                .name("Lee")
                .build();
        accountKim.setId(2L);

        Account account = Account.builder()
                .accountUser(accountLee)
                .balance(0L)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountKim))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account))
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "10000000000", 1000L));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 해지 계좌 사용")
    void useBalance_accountAlreadyUnRegistered() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(AccountStatus.UNREGISTERD)
                .balance(0L)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account))
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "10000000000", 1000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 거래 금액이 잔액보다 큰 경우")
    void useBalance_amountExceedBalance() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(900L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account))
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "10000000000", 1000L));

        // then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 너무 적은 금액인 경우")
    void useBalance_amountTooSmall() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(1_000_000_000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account))
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", minAmount));

        // then
        assertEquals(ErrorCode.AMOUNT_TOO_SMALL, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 너무 큰 금액인 경우")
    void useBalance_amountTooBig() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("Kim")
                .build();
        accountUser.setId(1L);

        Account account = Account.builder()
                .accountUser(accountUser)
                .balance(1_000_000_000L)
                .accountStatus(AccountStatus.IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser))
        ;

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account))
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", maxAmount));

        // then
        assertEquals(ErrorCode.AMOUNT_TOO_BIG, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 취소 - 성공")
    void cancelBalance_success() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Kim").build();
        user.setId(1L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.cancelBalance(
                "transactionId",
                "1000000000",
                CANCEL_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.S, captor.getValue().getTransactionResultType());
        assertEquals(TransactionType.CANCEL, captor.getValue().getTransactionType());
    }

    @Test
    @DisplayName("잔액 취소 - 존재하지 않는 거래")
    void cancelBalance_transactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty())
        ;

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 100L));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 취소 - 해당 계좌 없음")
    void cancelBalance_accountNotFound() {
        // given
        Transaction transaction = Transaction.builder().build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 10000L));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 취소 - 원거래 금액과 취소 금액이 다름")
    void cancelBalance_cancelAmountUnMatch() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Kim").build();
        user.setId(1L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.CANCEL)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId",
                        "1000000000",
                        CANCEL_AMOUNT)
        );

        // then
        assertEquals(ErrorCode.CANCEL_AMOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 확인 - 성공")
    void queryTransaction_success() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // when
        TransactionDto transactionDto = transactionService.queryTransactionId("trxId");

        // then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래 확인 - 원 거래 없음")
    void queryTransaction_transactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransactionId("f1f0536ae1d048e5be89e8c11815ce33"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}