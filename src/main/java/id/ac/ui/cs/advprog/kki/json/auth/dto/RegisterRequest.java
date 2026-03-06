package id.ac.ui.cs.advprog.kki.json.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 72)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    protected RegisterRequest() {}

    public RegisterRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
}