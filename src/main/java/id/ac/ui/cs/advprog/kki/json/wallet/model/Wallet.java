package id.ac.ui.cs.advprog.kki.json.wallet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private long balance = 0L;

    @Version
    private Long version;

    public Wallet(Long userId) {
        this.userId = userId;
        this.balance = 0L;
    }

    public Wallet(Long userId, long balance) {
        this.userId = userId;
        this.balance = balance;
    }
}
