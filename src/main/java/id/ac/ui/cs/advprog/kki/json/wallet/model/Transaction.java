package id.ac.ui.cs.advprog.kki.json.wallet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                // Idempotency guard for internal payment/refund calls.
                // Allows same referenceId for different types (PAYMENT vs REFUND).
                // Note: reference_id can be null; H2 allows multiple nulls in UNIQUE constraints.
                @jakarta.persistence.UniqueConstraint(
                        name = "uq_wallet_tx_user_type_ref",
                        columnNames = {"user_id", "type", "reference_id"}
                )
        },
        indexes = {
                @Index(name = "idx_wallet_transactions_user_id", columnList = "user_id"),
                @Index(name = "idx_wallet_transactions_created_at", columnList = "created_at"),
                @Index(name = "idx_wallet_transactions_reference_id", columnList = "reference_id")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 255)
    private String description;

    @Column(name = "failure_reason", length = 255)
    @Setter
    private String failureReason;

    @Column(name = "balance_before")
    @Setter
    private Long balanceBefore;

    @Column(name = "balance_after")
    @Setter
    private Long balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Transaction(Long userId,
                       TransactionType type,
                       long amount,
                       TransactionStatus status,
                       String referenceId,
                       String description) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        if (status != null) this.status = status;
        this.referenceId = referenceId;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
