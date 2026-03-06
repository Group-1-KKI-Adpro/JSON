package id.ac.ui.cs.advprog.kki.json.wallet.dto;

import jakarta.validation.constraints.Positive;

public record TopupRequest(@Positive long amount, String description) {
}

