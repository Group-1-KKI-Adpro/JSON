package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.KycResponse;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.KycApplication;
import id.ac.ui.cs.advprog.kki.json.model.KycStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.KycApplicationRepository;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class KycServiceImpl implements KycService {

    private final KycApplicationRepository kycApplicationRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public KycServiceImpl(KycApplicationRepository kycApplicationRepository,
                          UserRepository userRepository,
                          AuthService authService) {
        this.kycApplicationRepository = kycApplicationRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public KycResponse apply(String email, String fullName, String socialMediaLink) {
        User user = authService.getByEmail(email);

        if (user.getStatus() == AccountStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        if (user.getRole() == Role.JASTIPER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a Jastiper");
        }

        String cleanedFullName = fullName == null ? "" : fullName.trim();
        String cleanedSocialMediaLink =
                socialMediaLink == null || socialMediaLink.isBlank()
                        ? null
                        : socialMediaLink.trim();

        if (cleanedFullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        var existingApplication = kycApplicationRepository.findByUser(user);

        if (existingApplication.isPresent()) {
            KycApplication application = existingApplication.get();

            if (application.getStatus() == KycStatus.APPROVED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Application has already been approved"
                );
            }

            if (application.getStatus() == KycStatus.PENDING) {
                user.setStatus(AccountStatus.PENDING_VERIFICATION);
                userRepository.save(user);
                return toResponse(application);
            }

            application.setFullName(cleanedFullName);
            application.setSocialMediaLink(cleanedSocialMediaLink);
            application.setStatus(KycStatus.PENDING);
            application.setSubmittedAt(Instant.now());
            application.setReviewedAt(null);

            user.setStatus(AccountStatus.PENDING_VERIFICATION);
            userRepository.save(user);

            KycApplication saved = kycApplicationRepository.save(application);
            return toResponse(saved);
        }

        KycApplication application = new KycApplication(
                user,
                cleanedFullName,
                cleanedSocialMediaLink
        );

        user.setStatus(AccountStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        KycApplication saved = kycApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Override
    public List<KycResponse> getPendingApplications() {
        return kycApplicationRepository.findByStatus(KycStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public KycResponse approve(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        KycApplication application = kycApplicationRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found"));

        if (application.getStatus() != KycStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC application is not pending");
        }

        application.setStatus(KycStatus.APPROVED);
        application.setReviewedAt(Instant.now());

        user.setRole(Role.JASTIPER);
        user.setStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
        KycApplication saved = kycApplicationRepository.save(application);

        return toResponse(saved);
    }

    @Override
    public KycResponse reject(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        KycApplication application = kycApplicationRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found"));

        if (application.getStatus() != KycStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC application is not pending");
        }

        application.setStatus(KycStatus.REJECTED);
        application.setReviewedAt(Instant.now());

        user.setStatus(AccountStatus.ACTIVE);

        userRepository.save(user);
        KycApplication saved = kycApplicationRepository.save(application);

        return toResponse(saved);
    }

    private KycResponse toResponse(KycApplication application) {
        return new KycResponse(
                application.getId(),
                application.getUser().getId(),
                application.getFullName(),
                application.getSocialMediaLink(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getReviewedAt()
        );
    }
}