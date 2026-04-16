package id.ac.ui.cs.advprog.kki.json.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApplyKycRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Size(max = 255)
    private String socialMediaLink;

    protected ApplyKycRequest() {}

    public ApplyKycRequest(String fullName, String socialMediaLink) {
        this.fullName = fullName;
        this.socialMediaLink = socialMediaLink;
    }

    public String getFullName() { return fullName; }
    public String getSocialMediaLink() { return socialMediaLink; }
}