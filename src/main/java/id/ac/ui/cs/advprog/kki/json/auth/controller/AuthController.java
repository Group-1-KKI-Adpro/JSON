package id.ac.ui.cs.advprog.kki.json.auth.controller;

import id.ac.ui.cs.advprog.kki.json.auth.dto.AuthResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.LoginRequest;
import id.ac.ui.cs.advprog.kki.json.auth.dto.MeResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.RegisterRequest;
import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.kki.json.auth.security.JwtService;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public MeResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFullName()
        );
        return toMeResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);
        return toMeResponse(user);
    }

    @PatchMapping("/profile")
    public MeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                    Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User updated = authService.updateProfile(email, request.getUsername(), request.getFullName());
        return toMeResponse(updated);
    }

    private MeResponse toMeResponse(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );
    }
}