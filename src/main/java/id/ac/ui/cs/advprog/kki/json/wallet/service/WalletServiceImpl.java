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
}

