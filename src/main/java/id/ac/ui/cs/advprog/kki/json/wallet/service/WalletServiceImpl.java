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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ConcurrentMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public WalletServiceImpl(WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        requireUserId(userId);

        return withUserLock(userId, () ->
                walletRepository.findByUserId(userId).orElseGet(() -> {
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
                })
        );
    }

    @Override
    @Transactional
    public Transaction topup(Long userId, long amount, String description) {
        requirePositiveAmount(amount);

        return withUserLock(userId, () -> {
            Wallet wallet = getOrCreateWallet(userId);

            long before = wallet.getBalance();
            requireNonNegativeBalance(before);
            long after = safeAdd(before, amount);

            // Simulation: directly succeed.
            wallet.setBalance(after);
            walletRepository.save(wallet);

            Transaction tx = successTx(userId, TransactionType.TOPUP, amount, null, description, before, after);
            return transactionRepository.save(tx);
        });
    }

    @Override
    public List<Transaction> listTransactions(Long userId) {
        requireUserId(userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Transaction deduct(Long userId, long amount, String referenceId, String description) {
        requireUserId(userId);
        requirePositiveAmount(amount);
        requireReferenceId(referenceId);

        return withUserLock(userId, () -> {
            Transaction existing = findExistingByReference(userId, TransactionType.PAYMENT, referenceId);
            if (existing != null) {
                ensureIdempotentRequestMatches(existing, amount);
                return existing;
            }

            Wallet wallet = getOrCreateWallet(userId);
            long before = wallet.getBalance();
            requireNonNegativeBalance(before);
            if (before < amount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }

            long after = safeSubtract(before, amount);
            if (after < 0) {
                // Defensive; should be covered by before < amount.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }

            wallet.setBalance(after);
            walletRepository.save(wallet);

            Transaction tx = successTx(userId, TransactionType.PAYMENT, amount, referenceId, description, before, after);
            return saveIdempotentByReference(tx);
        });
    }

    @Override
    @Transactional
    public Transaction refund(Long userId, long amount, String referenceId, String description) {
        requireUserId(userId);
        requirePositiveAmount(amount);
        requireReferenceId(referenceId);

        return withUserLock(userId, () -> {
            Transaction existing = findExistingByReference(userId, TransactionType.REFUND, referenceId);
            if (existing != null) {
                ensureIdempotentRequestMatches(existing, amount);
                return existing;
            }

            Wallet wallet = getOrCreateWallet(userId);
            long before = wallet.getBalance();
            requireNonNegativeBalance(before);

            long after = safeAdd(before, amount);

            wallet.setBalance(after);
            walletRepository.save(wallet);

            Transaction tx = successTx(userId, TransactionType.REFUND, amount, referenceId, description, before, after);
            return saveIdempotentByReference(tx);
        });
    }

    @Override
    @Transactional
    public Transaction withdraw(Long userId, long amount, String description) {
        requireUserId(userId);
        requirePositiveAmount(amount);

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
        requireVerifyStatus(status);

        Transaction initial = transactionRepository.findByIdAndType(transactionId, TransactionType.WITHDRAW)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw transaction not found"));

        return withUserLock(initial.getUserId(), () -> {
            // State machine rules:
            // - Only WITHDRAW transactions can be verified here.
            // - Only PENDING can transition to SUCCESS/FAILED.
            // - SUCCESS triggers the actual wallet balance deduction.
            // - FAILED does not change balance.
            Transaction tx = transactionRepository.findByIdAndType(transactionId, TransactionType.WITHDRAW)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdraw transaction not found"));

            Transaction alreadyVerified = returnIfAlreadyVerified(tx, status);
            if (alreadyVerified != null) return alreadyVerified;
            if (status == TransactionStatus.FAILED) {
                return finalizeWithdrawAsFailed(tx, failureReason);
            }

            long amount = requireWithdrawAmount(tx);

            // The balance check is intentionally performed at verification time (not at request time),
            // so the system never commits a negative balance even if funds changed while pending.
            Wallet wallet = getOrCreateWallet(tx.getUserId());
            long before = wallet.getBalance();
            requireNonNegativeBalance(before);
            if (before < amount) {
                tx.setStatus(TransactionStatus.FAILED);
                tx.setFailureReason("Insufficient balance at verification time");
                return transactionRepository.save(tx);
            }

            long after = safeSubtract(before, amount);
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
        });
    }

    @Override
    public List<Transaction> listPendingWithdrawals() {
        return transactionRepository.findByTypeAndStatusOrderByCreatedAtAsc(
                TransactionType.WITHDRAW,
                TransactionStatus.PENDING
        );
    }

    private static Transaction returnIfAlreadyVerified(Transaction tx, TransactionStatus desiredStatus) {
        if (tx.getStatus() != TransactionStatus.PENDING) {
            if (tx.getStatus() == desiredStatus) return tx;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdraw transaction already verified");
        }
        return null;
    }

    private Transaction finalizeWithdrawAsFailed(Transaction tx, String failureReason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(failureReason == null ? "Rejected by admin" : failureReason);
        return transactionRepository.save(tx);
    }

    private static long requireWithdrawAmount(Transaction tx) {
        long amount = tx.getAmount();
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid withdrawal amount");
        }
        return amount;
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
    }

    private static void requirePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
    }

    private static void requireNonNegativeBalance(long balance) {
        if (balance < 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet balance is corrupted");
        }
    }

    private static void requireReferenceId(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referenceId is required");
        }
    }

    private static void requireVerifyStatus(TransactionStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (status == TransactionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be SUCCESS or FAILED");
        }
    }

    private static long safeAdd(long left, long right) {
        if (left < 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet balance is corrupted");
        }
        if (right < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }
    }

    private static long safeSubtract(long left, long right) {
        if (left < 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Wallet balance is corrupted");
        }
        if (right < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is too large");
        }
    }

    private Transaction findExistingByReference(Long userId, TransactionType type, String referenceId) {
        return transactionRepository
                .findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(userId, type, referenceId)
                .orElse(null);
    }

    private Transaction saveIdempotentByReference(Transaction tx) {
        if (tx.getReferenceId() == null) {
            return transactionRepository.save(tx);
        }

        try {
            return transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            // Another request with same (user,type,referenceId) already created the tx.
            Transaction created = findExistingByReference(tx.getUserId(), tx.getType(), tx.getReferenceId());
            if (created != null) {
                ensureIdempotentRequestMatches(created, tx.getAmount());
                return created;
            }
            throw e;
        }
    }

    private static void ensureIdempotentRequestMatches(Transaction existing, long requestedAmount) {
        if (existing.getAmount() != requestedAmount) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existing transaction reference does not match the requested amount"
            );
        }
    }

    private <T> T withUserLock(Long userId, Supplier<T> action) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private static Transaction successTx(Long userId,
                                        TransactionType type,
                                        long amount,
                                        String referenceId,
                                        String description,
                                        long before,
                                        long after) {
        Transaction tx = new Transaction(
                userId,
                type,
                amount,
                TransactionStatus.SUCCESS,
                referenceId,
                description
        );
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        return tx;
    }
}

