package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.KycResponse;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.KycApplication;
import id.ac.ui.cs.advprog.kki.json.model.KycStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.KycApplicationRepository;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private KycServiceImpl kycService;

    private User user;
    private KycApplication application;

    @BeforeEach
    void setUp() {
        user = new User("tara@gmail.com", "hashed-password", "Tara Nirmala Anwar");
        user.setUsername("tara");

        application = new KycApplication(user, "Tara Nirmala Anwar", "https://instagram.com/tara");
    }

    @Test
    void apply_success_setsUserPendingAndCreatesApplication() {
        when(authService.getByEmail("tara@gmail.com")).thenReturn(user);
        when(kycApplicationRepository.findByUser(user)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponse result = kycService.apply("tara@gmail.com", "Tara Nirmala Anwar", "https://instagram.com/tara");

        assertEquals("Tara Nirmala Anwar", result.getFullName());
        assertEquals(KycStatus.PENDING, result.getStatus());
        assertEquals(AccountStatus.PENDING_VERIFICATION, user.getStatus());
        verify(userRepository).save(user);
        verify(kycApplicationRepository).save(any(KycApplication.class));
    }

    @Test
    void apply_bannedUser_throwsForbidden() {
        user.setStatus(AccountStatus.BANNED);
        when(authService.getByEmail("tara@gmail.com")).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> kycService.apply("tara@gmail.com", "Tara Nirmala Anwar", null));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void apply_existingApplication_throwsConflict() {
        when(authService.getByEmail("tara@gmail.com")).thenReturn(user);
        when(kycApplicationRepository.findByUser(user)).thenReturn(Optional.of(application));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> kycService.apply("tara@gmail.com", "Tara Nirmala Anwar", null));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void getPendingApplications_returnsMappedResponses() {
        when(kycApplicationRepository.findByStatus(KycStatus.PENDING)).thenReturn(List.of(application));

        List<KycResponse> result = kycService.getPendingApplications();

        assertEquals(1, result.size());
        assertEquals(KycStatus.PENDING, result.get(0).getStatus());
        assertEquals("Tara Nirmala Anwar", result.get(0).getFullName());
    }

    @Test
    void approve_success_promotesUserToJastiper() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(kycApplicationRepository.findByUser(user)).thenReturn(Optional.of(application));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponse result = kycService.approve(1L);

        assertEquals(KycStatus.APPROVED, result.getStatus());
        assertEquals(Role.JASTIPER, user.getRole());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void reject_success_marksRejectedAndKeepsUserActive() {
        user.setStatus(AccountStatus.PENDING_VERIFICATION);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(kycApplicationRepository.findByUser(user)).thenReturn(Optional.of(application));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycResponse result = kycService.reject(1L);

        assertEquals(KycStatus.REJECTED, result.getStatus());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertNotNull(result.getReviewedAt());
    }
}