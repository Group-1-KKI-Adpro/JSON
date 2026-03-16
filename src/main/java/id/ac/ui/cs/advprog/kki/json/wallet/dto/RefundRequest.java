package id.ac.ui.cs.advprog.kki.json.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundRequest(
        @NotNull Long userId,
        @Positive long amount,
        @NotBlank String referenceId,
        String description
) {}
