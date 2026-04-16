package id.ac.ui.cs.advprog.kki.json.auth.dto;

import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;

public class UpdateUserStatusRequest {

    private Role role;
    private AccountStatus status;

    protected UpdateUserStatusRequest() {}

    public UpdateUserStatusRequest(Role role, AccountStatus status) {
        this.role = role;
        this.status = status;
    }

    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }
}