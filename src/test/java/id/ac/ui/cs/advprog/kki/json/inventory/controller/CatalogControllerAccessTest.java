package id.ac.ui.cs.advprog.kki.json.inventory.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.inventory.service.CatalogService;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerAccessTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogController(catalogService, authService)).build();
    }

    @Test
    void createCatalogItem_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/catalog")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pocky\",\"price\":123000,\"stock\":10}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        verifyNoInteractions(authService, catalogService);
    }

    @Test
    void createCatalogItem_pendingVerification_returnsForbidden() throws Exception {
        when(authService.getByEmail("jastiper@example.com")).thenReturn(user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.PENDING_VERIFICATION));

        mockMvc.perform(post("/api/catalog")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pocky\",\"price\":123000,\"stock\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only active users can access catalog management"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void getMyCatalogItems_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/catalog/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        verifyNoInteractions(authService, catalogService);
    }

    @Test
    void getMyCatalogItems_pendingVerification_returnsForbidden() throws Exception {
        when(authService.getByEmail("jastiper@example.com")).thenReturn(user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.PENDING_VERIFICATION));

        mockMvc.perform(get("/api/catalog/mine").principal(auth("jastiper@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only active users can access catalog management"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void updateCatalogItem_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/catalog/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":20}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        verifyNoInteractions(authService, catalogService);
    }

    @Test
    void updateCatalogItem_pendingVerification_returnsForbidden() throws Exception {
        when(authService.getByEmail("jastiper@example.com")).thenReturn(user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.PENDING_VERIFICATION));

        mockMvc.perform(patch("/api/catalog/1")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":20}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only active users can access catalog management"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void deleteCatalogItem_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/catalog/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));

        verifyNoInteractions(authService, catalogService);
    }

    @Test
    void deleteCatalogItem_pendingVerification_returnsForbidden() throws Exception {
        when(authService.getByEmail("jastiper@example.com")).thenReturn(user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.PENDING_VERIFICATION));

        mockMvc.perform(delete("/api/catalog/1").principal(auth("jastiper@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only active users can access catalog management"));

        verifyNoInteractions(catalogService);
    }

    private Authentication auth(String email) {
        return UsernamePasswordAuthenticationToken.authenticated(email, "ignored", List.of());
    }

    private User user(long id, String email, Role role, AccountStatus status) {
        User user = new User(email, "encoded-password", "Test User");
        user.setRole(role);
        user.setStatus(status);
        setUserId(user, id);
        return user;
    }

    private void setUserId(User user, long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
