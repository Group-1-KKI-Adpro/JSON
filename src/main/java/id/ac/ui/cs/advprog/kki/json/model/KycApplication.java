package id.ac.ui.cs.advprog.kki.json.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "kyc_applications")
public class KycApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 255)
    private String socialMediaLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus status;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    private Instant reviewedAt;

    protected KycApplication() {}

    public KycApplication(User user, String fullName, String socialMediaLink) {
        this.user = user;
        this.fullName = fullName;
        this.socialMediaLink = socialMediaLink;
        this.status = KycStatus.PENDING;
        this.submittedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFullName() { return fullName; }
    public String getSocialMediaLink() { return socialMediaLink; }
    public KycStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }

    public void setStatus(KycStatus status) { this.status = status; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}