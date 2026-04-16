package id.ac.ui.cs.advprog.kki.json.auth.dto;

import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;

public class PublicUserProfileResponse {

    private Long id;
    private String username;
    private String fullName;
    private Role role;
    private AccountStatus status;

    private Double averageRating;
    private Integer successfulTransactions;
    private Double successRate;

    public PublicUserProfileResponse(Long id, String username, String fullName,
                                     Role role, AccountStatus status,
                                     Double averageRating, Integer successfulTransactions, Double successRate) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.averageRating = averageRating;
        this.successfulTransactions = successfulTransactions;
        this.successRate = successRate;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public Double getAverageRating() { return averageRating; }
    public Integer getSuccessfulTransactions() { return successfulTransactions; }
    public Double getSuccessRate() { return successRate; }
}