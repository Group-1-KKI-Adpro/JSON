package id.ac.ui.cs.advprog.kki.json.wallet.service;

import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;
import id.ac.ui.cs.advprog.kki.json.wallet.repository.TransactionRepository;
import id.ac.ui.cs.advprog.kki.json.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        return walletRepository.findByUserId(userId).orElseGet(() -> {
            try {
                return walletRepository.save(new Wallet(userId));
            } catch (DataIntegrityViolationException e) {
                // Another request created it between find and save.
                return walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Failed to create wallet"
                        ));
            }
        });
    }

    @Override
    @Transactional
    public Transaction topup(Long userId, long amount, String description) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }

        Wallet wallet = getOrCreateWallet(userId);

        long before = wallet.getBalance();
        long after;
        try {
            after = Math.addExact(before, amount);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }

        // Simulation: directly succeed.
        wallet.setBalance(after);
        walletRepository.save(wallet);

        Transaction tx = new Transaction(
                userId,
                TransactionType.TOPUP,
                amount,
                TransactionStatus.SUCCESS,
                null,
                description
        );
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        return transactionRepository.save(tx);
    }

    @Override
    public List<Transaction> listTransactions(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Transaction deduct(Long userId, long amount, String referenceId, String description) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referenceId is required");
        }

        Optional<Transaction> existing = transactionRepository
                .findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(userId, TransactionType.PAYMENT, referenceId);
        if (existing != null && existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = getOrCreateWallet(userId);
        long before = wallet.getBalance();
        if (before < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        long after;
        try {
            after = Math.subtractExact(before, amount);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }
        if (after < 0) {
            // Defensive; should be covered by before < amount.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        wallet.setBalance(after);
        walletRepository.save(wallet);

        Transaction tx = new Transaction(
                userId,
                TransactionType.PAYMENT,
                amount,
                TransactionStatus.SUCCESS,
                referenceId,
                description
        );
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);

        try {
            return transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            // Another request with same (user,type,referenceId) already created the tx.
            Optional<Transaction> created = transactionRepository
                    .findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(userId, TransactionType.PAYMENT, referenceId);
            if (created != null && created.isPresent()) return created.get();
            throw e;
        }
    }

    @Override
    @Transactional
    public Transaction refund(Long userId, long amount, String referenceId, String description) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referenceId is required");
        }

        Optional<Transaction> existing = transactionRepository
                .findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(userId, TransactionType.REFUND, referenceId);
        if (existing != null && existing.isPresent()) {
            return existing.get();
        }

        Wallet wallet = getOrCreateWallet(userId);
        long before = wallet.getBalance();

        long after;
        try {
            after = Math.addExact(before, amount);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }

        wallet.setBalance(after);
        walletRepository.save(wallet);

        Transaction tx = new Transaction(
                userId,
                TransactionType.REFUND,
                amount,
                TransactionStatus.SUCCESS,
                referenceId,
                description
        );
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);

        try {
            return transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            Optional<Transaction> created = transactionRepository
                    .findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(userId, TransactionType.REFUND, referenceId);
            if (created != null && created.isPresent()) return created.get();
            throw e;
        }
    }

    @Override
    @Transactional
    public Transaction withdraw(Long userId, long amount, String description) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }

        // Per spec: creating a withdrawal creates a PENDING transaction; balance changes on admin verification.
        Transaction tx = new Transaction(
                userId,
                TransactionType.WITHDRAW,
                amount,
                TransactionStatus.PENDING,
                null,
                description
        );
        return transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public Transaction verifyWithdraw(Long transactionId, TransactionStatus status, String failureReason) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (status == TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be SUCCESS or FAILED");
        }

        // State machine rules:
        // - Only WITHDRAW transactions can be verified here.
        // - Only PENDING can transition to SUCCESS/FAILED.
        // - SUCCESS triggers the actual wallet balance deduction.
        // - FAILED does not change balance.
        Transaction tx = transactionRepository.findByIdAndType(transactionId, TransactionType.WITHDRAW)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw transaction not found"));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            if (tx.getStatus() == status) return tx;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdraw transaction already verified");
        }

        if (status == TransactionStatus.FAILED) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(failureReason == null ? "Rejected by admin" : failureReason);
            return transactionRepository.save(tx);
        }

        long amount = tx.getAmount();
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid withdrawal amount");
        }

        // The balance check is intentionally performed at verification time (not at request time),
        // so the system never commits a negative balance even if funds changed while pending.
        Wallet wallet = getOrCreateWallet(tx.getUserId());
        long before = wallet.getBalance();
        if (before < amount) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason("Insufficient balance at verification time");
            return transactionRepository.save(tx);
        }

        long after;
        try {
            after = Math.subtractExact(before, amount);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }
        if (after < 0) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason("Insufficient balance at verification time");
            return transactionRepository.save(tx);
        }

        wallet.setBalance(after);
        walletRepository.save(wallet);

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setFailureReason(null);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        return transactionRepository.save(tx);
    }
}

