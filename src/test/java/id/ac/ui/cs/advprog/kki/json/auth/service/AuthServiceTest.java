package id.ac.ui.cs.advprog.kki.json.auth.service;

import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
    void register_success_generatesUsernameAndSavesUser() {
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.existsByUsername("tara")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register("tara@gmail.com", "password123", null);

        assertEquals("tara@gmail.com", result.getEmail());
        assertEquals("hashed-password", result.getPasswordHash());
        assertEquals("tara", result.getUsername());
        assertNull(result.getFullName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register("tara@gmail.com", "password123", null));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void login_success_returnsUser() {
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        User result = authService.login("tara@gmail.com", "password123");

        assertEquals(user, result);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed-password")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("tara@gmail.com", "wrongpass"));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void login_bannedUser_throwsForbidden() {
        user.setStatus(AccountStatus.BANNED);
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("tara@gmail.com", "password123"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void updateProfile_blankUsername_autoGeneratesUsername() {
        User userWithoutUsername = new User("newuser@gmail.com", "hashed-password", "New User");

        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.of(userWithoutUsername));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.updateProfile("newuser@gmail.com", "   ", "Updated Name");

        assertEquals("newuser", result.getUsername());
        assertEquals("Updated Name", result.getFullName());
    }

    @Test
    void updateProfile_duplicateUsername_throwsConflict() {
        when(userRepository.findByEmail("tara@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("takenusername")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile("tara@gmail.com", "takenusername", "Tara"));

        assertEquals(409, ex.getStatusCode().value());
    }
}