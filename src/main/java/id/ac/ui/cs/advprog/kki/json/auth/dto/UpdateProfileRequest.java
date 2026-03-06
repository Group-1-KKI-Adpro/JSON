package id.ac.ui.cs.advprog.kki.json.auth.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 60)
    private String username;

    @Size(max = 100)
    private String fullName;

    protected UpdateProfileRequest() {}

    public UpdateProfileRequest(String username, String fullName) {
        this.username = username;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
}