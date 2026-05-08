package id.ac.ui.cs.advprog.kki.json.wallet;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;
import id.ac.ui.cs.advprog.kki.json.wallet.repository.TransactionRepository;
import id.ac.ui.cs.advprog.kki.json.wallet.repository.WalletRepository;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {


    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet(1L, 100L);
    }

    @Test
    void GetWhenWalletExist() {
        //Should Pass Because it Exist
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        Wallet result = walletService.getOrCreateWallet(1L);
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(100L, result.getBalance());
        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));


    }

    @Test
    void CreateWalletWhenNotExist() {
        //Will make it up when it doesnt

        when(walletRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result= walletService.getOrCreateWallet(2L);

        assertNotNull(result);
        assertEquals(2L, result.getUserId());
        assertEquals(0L, result.getBalance());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).findByUserId(2L);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(2L, walletCaptor.getValue().getUserId());
        assertEquals(0L, walletCaptor.getValue().getBalance());
    }

    @Test
    void GetOrCreateWalletWhenUserIdIsNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.getOrCreateWallet(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verifyNoInteractions(walletRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void CreateWalletWhenDuplicateOccursShouldRetryFindAndReturnExisting() {
        Wallet existing = new Wallet(3L, 500L);

        when(walletRepository.findByUserId(3L))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(walletRepository.save(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Wallet result = walletService.getOrCreateWallet(3L);

        assertSame(existing, result);
        assertEquals(3L, result.getUserId());
        assertEquals(500L, result.getBalance());

        verify(walletRepository, times(2)).findByUserId(3L);
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void TopupWhenAmountIsNotPositiveShouldThrowBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.topup(1L, 0L, "x"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verifyNoInteractions(walletRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void TopupShouldUpdateWalletAndCreateTransactionRecord() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.topup(1L, 50L, "topup");

        assertNotNull(tx);
        assertEquals(1L, tx.getUserId());
        assertEquals(TransactionType.TOPUP, tx.getType());
        assertEquals(50L, tx.getAmount());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(100L, tx.getBalanceBefore());
        assertEquals(150L, tx.getBalanceAfter());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).findByUserId(1L);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(150L, walletCaptor.getValue().getBalance());

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertEquals(1L, txCaptor.getValue().getUserId());
        assertEquals(TransactionType.TOPUP, txCaptor.getValue().getType());
        assertEquals(TransactionStatus.SUCCESS, txCaptor.getValue().getStatus());
        assertEquals(100L, txCaptor.getValue().getBalanceBefore());
        assertEquals(150L, txCaptor.getValue().getBalanceAfter());
    }

    @Test
    void TopupWhenAmountOverflowsShouldThrowBadRequestAndNotPersist() {
        Wallet maxWallet = new Wallet(1L, Long.MAX_VALUE);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(maxWallet));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.topup(1L, 1L, "overflow"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(transactionRepository);
    }

    // Spec tests below: expected to FAIL until deduct/refund/withdraw are implemented.

    @Test
    void DeductShouldDecreaseBalanceAndCreateTransactionRecord() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.deduct(1L, 40L, "order-1", "payment");

        assertNotNull(tx);
        assertEquals(1L, tx.getUserId());
        assertEquals(TransactionType.PAYMENT, tx.getType());
        assertEquals(40L, tx.getAmount());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(100L, tx.getBalanceBefore());
        assertEquals(60L, tx.getBalanceAfter());
        assertEquals("order-1", tx.getReferenceId());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(60L, walletCaptor.getValue().getBalance());
    }

    @Test
    void DeductWhenInsufficientBalanceShouldThrowBadRequestAndNotPersist() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletService.deduct(1L, 1000L, "order-2", "payment"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void RefundShouldIncreaseBalanceAndCreateTransactionRecord() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.refund(1L, 25L, "order-1", "refund");

        assertNotNull(tx);
        assertEquals(1L, tx.getUserId());
        assertEquals(TransactionType.REFUND, tx.getType());
        assertEquals(25L, tx.getAmount());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(100L, tx.getBalanceBefore());
        assertEquals(125L, tx.getBalanceAfter());
        assertEquals("order-1", tx.getReferenceId());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(125L, walletCaptor.getValue().getBalance());
    }

    @Test
    void WithdrawShouldCreatePendingTransactionAndNotAllowNegativeBalance() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.withdraw(1L, 10L, "withdraw");

        assertNotNull(tx);
        assertEquals(1L, tx.getUserId());
        assertEquals(TransactionType.WITHDRAW, tx.getType());
        assertEquals(10L, tx.getAmount());
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
    }

    @Test
    void VerifyWithdrawSuccessShouldDeductBalanceAndMarkTransactionSuccess() {
        Transaction pending = new Transaction(1L, TransactionType.WITHDRAW, 30L, TransactionStatus.PENDING, null, "w");

        when(transactionRepository.findByIdAndType(10L, TransactionType.WITHDRAW)).thenReturn(Optional.of(pending));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.verifyWithdraw(10L, TransactionStatus.SUCCESS, null);

        assertSame(pending, tx);
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(100L, tx.getBalanceBefore());
        assertEquals(70L, tx.getBalanceAfter());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(70L, walletCaptor.getValue().getBalance());

        verify(transactionRepository).save(pending);
    }

    @Test
    void VerifyWithdrawFailedShouldNotChangeBalance() {
        Transaction pending = new Transaction(1L, TransactionType.WITHDRAW, 30L, TransactionStatus.PENDING, null, "w");

        when(transactionRepository.findByIdAndType(10L, TransactionType.WITHDRAW)).thenReturn(Optional.of(pending));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.verifyWithdraw(10L, TransactionStatus.FAILED, "nope");

        assertSame(pending, tx);
        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertEquals("nope", tx.getFailureReason());

        verifyNoInteractions(walletRepository);
        verify(transactionRepository).save(pending);
    }

    @Test
    void VerifyWithdrawWhenInsufficientBalanceAtVerificationShouldFailAndNotPersistWallet() {
        Wallet low = new Wallet(1L, 10L);
        Transaction pending = new Transaction(1L, TransactionType.WITHDRAW, 30L, TransactionStatus.PENDING, null, "w");

        when(transactionRepository.findByIdAndType(10L, TransactionType.WITHDRAW)).thenReturn(Optional.of(pending));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(low));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction tx = walletService.verifyWithdraw(10L, TransactionStatus.SUCCESS, null);

        assertSame(pending, tx);
        assertEquals(TransactionStatus.FAILED, tx.getStatus());
        assertNotNull(tx.getFailureReason());

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository).save(pending);
    }

    @Test
    void VerifyWithdrawWhenAlreadyVerifiedWithSameStatusShouldBeIdempotent() {
        Transaction verified = new Transaction(1L, TransactionType.WITHDRAW, 30L, TransactionStatus.SUCCESS, null, "w");
        when(transactionRepository.findByIdAndType(10L, TransactionType.WITHDRAW)).thenReturn(Optional.of(verified));

        Transaction tx = walletService.verifyWithdraw(10L, TransactionStatus.SUCCESS, null);

        assertSame(verified, tx);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(walletRepository);
    }
}
