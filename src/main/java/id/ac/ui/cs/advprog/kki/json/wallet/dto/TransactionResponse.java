package id.ac.ui.cs.advprog.kki.json.wallet.dto;

import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;

import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long userId,
        TransactionType type,
        long amount,
        TransactionStatus status,
        Instant timestamp,
        String description
) {
}

