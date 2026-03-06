package id.ac.ui.cs.advprog.kki.json.auth.dto;

import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import lombok.Getter;

@Getter
public class MeResponse {

    private Long id;
    private String email;
    private String username;
    private String fullName;
    private Role role;
    private AccountStatus status;

    public MeResponse(Long id, String email, String username, String fullName, Role role, AccountStatus status) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

}