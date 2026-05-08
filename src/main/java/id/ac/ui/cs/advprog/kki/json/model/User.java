package id.ac.ui.cs.advprog.kki.json.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_username", columnList = "username", unique = true)
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(unique = true, length = 60)
    private String username;

    @Column(length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Integer successfulTransactions = 0;

    @Column(nullable = false)
    private Integer totalTransactions = 0;

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Integer ratingCount = 0;

    @Version
    private Long version;

    protected User() {}

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = Role.TITIPER;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Integer getSuccessfulTransactions() { return successfulTransactions; }
    public Integer getTotalTransactions() { return totalTransactions; }
    public Double getAverageRating() { return averageRating; }
    public Integer getRatingCount() { return ratingCount; }
    public Long getVersion() { return version; }

    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(Role role) { this.role = role; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setSuccessfulTransactions(Integer successfulTransactions) { this.successfulTransactions = successfulTransactions; }
    public void setTotalTransactions(Integer totalTransactions) { this.totalTransactions = totalTransactions; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
}