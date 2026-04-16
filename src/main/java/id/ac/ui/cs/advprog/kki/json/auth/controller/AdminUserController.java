package id.ac.ui.cs.advprog.kki.json.auth.controller;

import id.ac.ui.cs.advprog.kki.json.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateUserStatusRequest;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/admin/users")
    public List<AdminUserResponse> getAllUsers() {
        return authService.getAllUsers();
    }

    @PatchMapping("/api/admin/users/{userId}")
    public AdminUserResponse updateUser(@PathVariable Long userId,
                                        @Valid @RequestBody UpdateUserStatusRequest request) {
        return authService.updateUserByAdmin(userId, request.getRole(), request.getStatus());
    }
}