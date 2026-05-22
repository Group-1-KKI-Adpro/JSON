package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.KycResponse;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.KycApplication;
import id.ac.ui.cs.advprog.kki.json.model.KycStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.KycApplicationRepository;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class KycServiceImpl implements KycService {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);

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
        long startTime = System.nanoTime();
        String outcome = "STARTED";
        Long userId = null;

        try {
            User user = authService.getByEmail(email);
            userId = user.getId();

            if (user.getStatus() == AccountStatus.BANNED) {
                outcome = "REJECTED_BANNED_ACCOUNT";
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
            }

            if (user.getRole() == Role.JASTIPER) {
                outcome = "REJECTED_ALREADY_JASTIPER";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a Jastiper");
            }

            String cleanedFullName = fullName == null ? "" : fullName.trim();
            String cleanedSocialMediaLink =
                    socialMediaLink == null || socialMediaLink.isBlank()
                            ? null
                            : socialMediaLink.trim();

            if (cleanedFullName.isBlank()) {
                outcome = "REJECTED_EMPTY_FULL_NAME";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
            }

            var existingApplication = kycApplicationRepository.findByUser(user);

            if (existingApplication.isPresent()) {
                KycApplication application = existingApplication.get();

                if (application.getStatus() == KycStatus.PENDING) {
                    user.setStatus(AccountStatus.PENDING_VERIFICATION);
                    userRepository.save(user);

                    outcome = "EXISTING_PENDING_APPLICATION";
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

                outcome = "RESUBMITTED_APPLICATION";
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

            outcome = "CREATED_APPLICATION";
            return toResponse(saved);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info(
                    "observability_kyc_apply email={} userId={} outcome={} durationMs={}",
                    email,
                    userId,
                    outcome,
                    durationMs
            );
        }
    }

    @Override
    public List<KycResponse> getPendingApplications() {
        long startTime = System.nanoTime();

        try {
            List<KycResponse> responses = kycApplicationRepository.findByStatus(KycStatus.PENDING)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info(
                    "observability_kyc_pending_list count={} durationMs={}",
                    responses.size(),
                    durationMs
            );

            return responses;
        } catch (RuntimeException e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.warn(
                    "observability_kyc_pending_list_failed durationMs={} error={}",
                    durationMs,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public KycResponse approve(Long userId) {
        long startTime = System.nanoTime();
        String outcome = "STARTED";

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("observability_kyc_approve userId={} outcome=USER_NOT_FOUND", userId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                    });

            KycApplication application = kycApplicationRepository.findByUser(user)
                    .orElseThrow(() -> {
                        log.warn("observability_kyc_approve userId={} outcome=APPLICATION_NOT_FOUND", userId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found");
                    });

            if (application.getStatus() != KycStatus.PENDING) {
                outcome = "REJECTED_NOT_PENDING";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC application is not pending");
            }

            application.setStatus(KycStatus.APPROVED);
            application.setReviewedAt(Instant.now());

            user.setRole(Role.JASTIPER);
            user.setStatus(AccountStatus.ACTIVE);

            userRepository.save(user);
            KycApplication saved = kycApplicationRepository.save(application);

            outcome = "APPROVED";
            return toResponse(saved);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info(
                    "observability_kyc_approve userId={} outcome={} durationMs={}",
                    userId,
                    outcome,
                    durationMs
            );
        }
    }

    @Override
    public KycResponse reject(Long userId) {
        long startTime = System.nanoTime();
        String outcome = "STARTED";

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("observability_kyc_reject userId={} outcome=USER_NOT_FOUND", userId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                    });

            KycApplication application = kycApplicationRepository.findByUser(user)
                    .orElseThrow(() -> {
                        log.warn("observability_kyc_reject userId={} outcome=APPLICATION_NOT_FOUND", userId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC application not found");
                    });

            if (application.getStatus() != KycStatus.PENDING) {
                outcome = "REJECTED_NOT_PENDING";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC application is not pending");
            }

            application.setStatus(KycStatus.REJECTED);
            application.setReviewedAt(Instant.now());

            user.setStatus(AccountStatus.ACTIVE);

            userRepository.save(user);
            KycApplication saved = kycApplicationRepository.save(application);

            outcome = "REJECTED";
            return toResponse(saved);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info(
                    "observability_kyc_reject userId={} outcome={} durationMs={}",
                    userId,
                    outcome,
                    durationMs
            );
        }
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


//This commit adds structured monitoring logs to the Jastiper application/KYC flow in the Authentication & Profile module. The logs capture application submission, resubmission, pending application listing, admin approval, and admin rejection. Each log records the user/application identifier, operation outcome, and duration in milliseconds. These logs are relevant because KYC approval is a critical role-change flow; failures, repeated submissions, slow processing, or rejected approvals can directly affect whether users can become Jastipers and access catalog creation.
