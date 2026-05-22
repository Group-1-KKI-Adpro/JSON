package id.ac.ui.cs.advprog.kki.json.config;

import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
        name = "app.demo.seed.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DemoDataInitializer {
    @Bean
    public ApplicationRunner seedDemoUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "admin@json.local",
                    "admin123",
                    "admin",
                    "JSON Demo Admin",
                    Role.ADMIN,
                    AccountStatus.ACTIVE,
                    0,
                    0,
                    0.0,
                    0
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "titiper@json.local",
                    "titiper123",
                    "titiper",
                    "JSON Demo Titiper",
                    Role.TITIPER,
                    AccountStatus.ACTIVE,
                    0,
                    0,
                    0.0,
                    0
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    "jastiper@json.local",
                    "jastiper123",
                    "jastiper",
                    "JSON Demo Jastiper",
                    Role.JASTIPER,
                    AccountStatus.ACTIVE,
                    3,
                    3,
                    4.7,
                    3
            );
        };
    }

    private void createUserIfMissing(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            String username,
            String fullName,
            Role role,
            AccountStatus status,
            Integer successfulTransactions,
            Integer totalTransactions,
            Double averageRating,
            Integer ratingCount
    ) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User(
                email,
                passwordEncoder.encode(rawPassword),
                fullName
        );

        user.setUsername(makeUniqueUsername(userRepository, username));
        user.setRole(role);
        user.setStatus(status);
        user.setSuccessfulTransactions(successfulTransactions);
        user.setTotalTransactions(totalTransactions);
        user.setAverageRating(averageRating);
        user.setRatingCount(ratingCount);

        userRepository.save(user);
    }

    private String makeUniqueUsername(UserRepository userRepository, String baseUsername) {
        String cleaned = baseUsername
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "");

        if (cleaned.isBlank()) {
            cleaned = "user";
        }

        if (!userRepository.existsByUsername(cleaned)) {
            return cleaned;
        }

        for (int i = 1; i <= 9999; i++) {
            String candidate = cleaned + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Cannot generate unique demo username");
    }
}