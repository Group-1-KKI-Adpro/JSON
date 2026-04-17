package id.ac.ui.cs.advprog.kki.json.auth.controller;

import id.ac.ui.cs.advprog.kki.json.auth.dto.ApplyKycRequest;
import id.ac.ui.cs.advprog.kki.json.auth.dto.KycResponse;
import id.ac.ui.cs.advprog.kki.json.auth.service.KycService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/api/kyc/apply")
    public KycResponse apply(@Valid @RequestBody ApplyKycRequest request,
                             Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return kycService.apply(email, request.getFullName(), request.getSocialMediaLink());
    }

    @GetMapping("/api/admin/kyc/pending")
    public List<KycResponse> getPendingApplications() {
        return kycService.getPendingApplications();
    }

    @PostMapping("/api/admin/kyc/{userId}/approve")
    public KycResponse approve(@PathVariable Long userId) {
        return kycService.approve(userId);
    }

    @PostMapping("/api/admin/kyc/{userId}/reject")
    public KycResponse reject(@PathVariable Long userId) {
        return kycService.reject(userId);
    }
}