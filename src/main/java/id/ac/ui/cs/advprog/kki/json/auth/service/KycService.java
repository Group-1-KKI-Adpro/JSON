package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.KycResponse;

import java.util.List;

public interface KycService {
    KycResponse apply(String email, String fullName, String socialMediaLink);
    List<KycResponse> getPendingApplications();
    KycResponse approve(Long userId);
    KycResponse reject(Long userId);
}