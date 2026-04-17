package id.ac.ui.cs.advprog.kki.json.wallet.service;

import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;

import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(Long userId);

    Transaction topup(Long userId, long amount, String description);

    List<Transaction> listTransactions(Long userId);

    Transaction deduct(Long userId, long amount, String referenceId, String description);

    Transaction refund(Long userId, long amount, String referenceId, String description);

    Transaction withdraw(Long userId, long amount, String description);

    Transaction verifyWithdraw(Long transactionId, TransactionStatus status, String failureReason);
}

