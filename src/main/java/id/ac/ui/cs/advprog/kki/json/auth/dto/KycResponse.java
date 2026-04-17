package id.ac.ui.cs.advprog.kki.json.auth.dto;

import id.ac.ui.cs.advprog.kki.json.model.KycStatus;

import java.time.Instant;

public class KycResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String socialMediaLink;
    private KycStatus status;
    private Instant submittedAt;
    private Instant reviewedAt;

    public KycResponse(Long id, Long userId, String fullName, String socialMediaLink,
                       KycStatus status, Instant submittedAt, Instant reviewedAt) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.socialMediaLink = socialMediaLink;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getSocialMediaLink() { return socialMediaLink; }
    public KycStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}