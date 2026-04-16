package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.AdminUserResponse;
import id.ac.ui.cs.advprog.kki.json.auth.dto.PublicUserProfileResponse;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceMonitoringTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("tara@gmail.com", "hashed-password", "Tara");
        user.setUsername("tara");
    }

    @Test
    void getPublicProfile_forTitiper_returnsBasicInfo() {
        when(userRepository.findByUsername("tara")).thenReturn(Optional.of(user));

        PublicUserProfileResponse result = authService.getPublicProfile("tara");

        assertEquals("tara", result.getUsername());
        assertEquals(Role.TITIPER, result.getRole());
        assertNull(result.getAverageRating());
        assertNull(result.getSuccessfulTransactions());
        assertNull(result.getSuccessRate());
    }

    @Test
    void getPublicProfile_forJastiper_returnsPlaceholderStats() {
        user.setRole(Role.JASTIPER);
        when(userRepository.findByUsername("tara")).thenReturn(Optional.of(user));

        PublicUserProfileResponse result = authService.getPublicProfile("tara");

        assertEquals(Role.JASTIPER, result.getRole());
        assertEquals(0.0, result.getAverageRating());
        assertEquals(0, result.getSuccessfulTransactions());
        assertEquals(0.0, result.getSuccessRate());
    }

    @Test
    void getAllUsers_returnsMappedAdminResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<AdminUserResponse> result = authService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("tara@gmail.com", result.get(0).getEmail());
    }

    @Test
    void updateUserByAdmin_updatesRoleAndStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse result = authService.updateUserByAdmin(1L, Role.JASTIPER, AccountStatus.ACTIVE);

        assertEquals(Role.JASTIPER, result.getRole());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
    }

    @Test
    void requireActiveUser_bannedUser_throwsForbidden() {
        user.setStatus(AccountStatus.BANNED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.requireActiveUser(1L));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void requireRole_wrongRole_throwsForbidden() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.requireRole(1L, Role.JASTIPER));

        assertEquals(403, ex.getStatusCode().value());
    }
}