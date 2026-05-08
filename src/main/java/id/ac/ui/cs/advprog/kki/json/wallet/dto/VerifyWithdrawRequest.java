package id.ac.ui.cs.advprog.kki.json.wallet.dto;

import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import jakarta.validation.constraints.NotNull;

// Admin verification request for a withdrawal transaction.
// status must be SUCCESS or FAILED; PENDING is rejected by the service.
public record VerifyWithdrawRequest(
        @NotNull TransactionStatus status,
        String failureReason
) {}
