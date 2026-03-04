package id.ac.ui.cs.advprog.kki.json.auth.dto;

public class AuthResponse {

    private String token;

    public AuthResponse(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
}