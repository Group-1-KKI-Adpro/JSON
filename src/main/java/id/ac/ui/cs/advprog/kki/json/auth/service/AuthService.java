package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.PublicUserProfileResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateReputationRequest;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String password, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String hashedPassword = passwordEncoder.encode(password);

        String cleanedFullName = (fullName == null || fullName.isBlank())
                ? null
                : fullName.trim();

        User user = new User(email, hashedPassword, cleanedFullName);
        user.setUsername(autoUsernameFromEmail(email));

        return userRepository.save(user);
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (user.getStatus() == AccountStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return user;
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    public User requireActiveUser(Long userId) {
        User user = getById(userId);

        if (user.getStatus() == AccountStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is banned");
        }

        return user;
    }

    public User requireRole(Long userId, Role role) {
        User user = getById(userId);

        if (user.getRole() != role) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User does not have required role"
            );
        }

        return user;
    }

    public User updateProfile(String email, String username, String fullName) {
        User user = getByEmail(email);

        if (user.getStatus() == AccountStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }

        String finalUsername = username;

        if (finalUsername == null || finalUsername.isBlank()) {
            finalUsername = autoUsernameFromEmail(user.getEmail());
        } else {
            finalUsername = finalUsername.trim();
        }

        if (finalUsername != null && !finalUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(finalUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
            }

            user.setUsername(finalUsername);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void updateJastiperReputation(Long jastiperId, UpdateReputationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be null");
        }

        User jastiper = userRepository.findByIdForUpdate(jastiperId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Jastiper not found"
                ));

        if (jastiper.getRole() != Role.JASTIPER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User is not a Jastiper"
            );
        }

        if (jastiper.getStatus() == AccountStatus.BANNED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Jastiper account is banned"
            );
        }

        if (Boolean.TRUE.equals(request.getTransactionCompleted())) {
            jastiper.setTotalTransactions(jastiper.getTotalTransactions() + 1);

            if (Boolean.TRUE.equals(request.getTransactionSuccessful())) {
                jastiper.setSuccessfulTransactions(
                        jastiper.getSuccessfulTransactions() + 1
                );
            }
        }

        Integer rating = request.getJastiperRating();

        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Jastiper rating must be between 1 and 5"
                );
            }

            double currentTotalScore =
                    jastiper.getAverageRating() * jastiper.getRatingCount();

            int newRatingCount = jastiper.getRatingCount() + 1;
            double newAverage = (currentTotalScore + rating) / newRatingCount;

            jastiper.setRatingCount(newRatingCount);
            jastiper.setAverageRating(newAverage);
        }

        userRepository.save(jastiper);
    }

    public PublicUserProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getRole() == Role.JASTIPER) {
            double successRate = user.getTotalTransactions() > 0
                    ? ((double) user.getSuccessfulTransactions()
                    / user.getTotalTransactions()) * 100
                    : 0.0;

            return new PublicUserProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole(),
                    user.getStatus(),
                    user.getAverageRating(),
                    user.getSuccessfulTransactions(),
                    user.getTotalTransactions(),
                    successRate,
                    user.getRatingCount()
            );
        }

        return new PublicUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    public AdminUserResponse updateUserByAdmin(Long userId, Role role, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (role != null) {
            if (role == Role.ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot assign ADMIN role from this endpoint"
                );
            }

            user.setRole(role);
        }

        if (status != null) {
            user.setStatus(status);
        }

        User saved = userRepository.save(user);

        return toAdminUserResponse(saved);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );
    }

    private String autoUsernameFromEmail(String email) {
        String base = email.split("@")[0].toLowerCase();

        base = base.replaceAll("[^a-z0-9._-]", "");

        if (base.length() < 3) {
            base = base + "user";
        }

        String candidate = base.length() > 60
                ? base.substring(0, 60)
                : base;

        if (!userRepository.existsByUsername(candidate)) {
            return candidate;
        }

        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int maxBaseLen = 60 - suffix.length();

            String trimmed = candidate.length() > maxBaseLen
                    ? candidate.substring(0, maxBaseLen)
                    : candidate;

            String attempt = trimmed + suffix;

            if (!userRepository.existsByUsername(attempt)) {
                return attempt;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot generate unique username"
        );
    }
}