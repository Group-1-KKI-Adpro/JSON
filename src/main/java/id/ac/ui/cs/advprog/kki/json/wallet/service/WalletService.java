package id.ac.ui.cs.advprog.kki.json.wallet.service;

import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;

import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(Long userId);

    Transaction topup(Long userId, long amount, String description);

    List<Transaction> listTransactions(Long userId);
}

