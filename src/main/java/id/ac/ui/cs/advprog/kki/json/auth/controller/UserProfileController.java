package id.ac.ui.cs.advprog.kki.json.auth.controller;

import id.ac.ui.cs.advprog.kki.json.auth.dto.PublicUserProfileResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateReputationRequest;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserProfileController {

    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/users/{username}")
    public PublicUserProfileResponse getPublicProfile(@PathVariable String username) {
        return authService.getPublicProfile(username);
    }

    @GetMapping("/api/users/id/{userId}")
    public PublicUserProfileResponse getPublicProfileById(@PathVariable Long userId) {
        User user = authService.getById(userId);
        return authService.getPublicProfile(user.getUsername());
    }

    @PatchMapping("/api/internal/users/{userId}/reputation")
    public void updateReputationForInternalTesting(
            @PathVariable Long userId,
            @RequestBody UpdateReputationRequest request
    ) {
        authService.updateJastiperReputation(userId, request);
    }
}