package id.ac.ui.cs.advprog.kki.json.inventory.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogController(catalogService, authService)).build();
    }

    private CatalogItem item(int id, int jastiperId, String name, int stock) {
        CatalogItem item = new CatalogItem();
        item.setJastiperId(jastiperId);
        item.setName(name);
        item.setDescription("Sample description");
        item.setPrice(123000);
        item.setStock(stock);
        item.setOrigin("Japan");
        item.setPurchaseDate("2026-05-01");
        setId(item, id);
        return item;
    }

    private User user(long id, String email, Role role, AccountStatus status) {
        User user = new User(email, "encoded-password", "Test User");
        user.setRole(role);
        user.setStatus(status);
        setUserId(user, id);
        return user;
    }

    private Authentication auth(String email) {
        return UsernamePasswordAuthenticationToken.authenticated(email, "ignored", List.of());
    }

    private void setId(CatalogItem item, int id) {
        try {
            Field field = CatalogItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.setInt(item, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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

    @Test
    void createCatalogItem_success_usesAuthenticatedJastiperIdAndReturnsCreatedItem() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.createCatalogItemForJastiper(any(CatalogItemRequest.class), eq(42)))
                .thenReturn(item(1, 42, "Pocky", 10));

        mockMvc.perform(post("/api/catalog")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"jastiperId\":999,"
                                + "\"name\":\"Pocky\","
                                + "\"description\":\"Sample\","
                                + "\"price\":123000,"
                                + "\"stock\":10,"
                                + "\"origin\":\"Japan\","
                                + "\"purchaseDate\":\"2026-05-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.jastiperId").value(42))
                .andExpect(jsonPath("$.name").value("Pocky"));

        verify(catalogService).createCatalogItemForJastiper(any(CatalogItemRequest.class), eq(42));
    }

    @Test
    void createCatalogItem_titiper_returnsForbidden() throws Exception {
        User currentUser = user(7L, "buyer@example.com", Role.TITIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("buyer@example.com")).thenReturn(currentUser);

        mockMvc.perform(post("/api/catalog")
                        .principal(auth("buyer@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pocky\",\"price\":123000,\"stock\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only approved Jastipers can create catalog items"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void createCatalogItem_invalidInput_returnsBadRequest() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.createCatalogItemForJastiper(any(CatalogItemRequest.class), eq(42)))
                .thenThrow(new IllegalArgumentException("Name cannot be empty"));

        mockMvc.perform(post("/api/catalog")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":123000,\"stock\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name cannot be empty"));
    }

    @Test
    void getAllCatalogItems_returnsArray() throws Exception {
        when(catalogService.getAllCatalogItems()).thenReturn(List.of(item(1, 42, "Pocky", 10), item(2, 43, "KitKat", 5)));

        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pocky"))
                .andExpect(jsonPath("$[1].name").value("KitKat"));
    }

    @Test
    void searchCatalogItems_blankKeyword_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/catalog/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("keyword is required"));
    }

    @Test
    void searchCatalogItems_returnsMatchingItems() throws Exception {
        when(catalogService.searchCatalogItems("snack")).thenReturn(List.of(item(1, 42, "Pocky", 10)));

        mockMvc.perform(get("/api/catalog/search").param("keyword", "snack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pocky"));
    }

    @Test
    void getCatalogItemsByJastiperId_returnsArray() throws Exception {
        when(catalogService.getCatalogItemsByJastiperId(7)).thenReturn(List.of(item(1, 7, "Pocky", 10)));

        mockMvc.perform(get("/api/catalog/jastiper/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jastiperId").value(7));
    }

    @Test
    void getMyCatalogItems_success_returnsOwnItems() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemsByJastiperId(42)).thenReturn(List.of(item(1, 42, "Pocky", 10)));

        mockMvc.perform(get("/api/catalog/mine").principal(auth("jastiper@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jastiperId").value(42));
    }

    @Test
    void getMyCatalogItems_titiper_returnsForbidden() throws Exception {
        User currentUser = user(7L, "buyer@example.com", Role.TITIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("buyer@example.com")).thenReturn(currentUser);

        mockMvc.perform(get("/api/catalog/mine").principal(auth("buyer@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only approved Jastipers can create catalog items"));

        verifyNoInteractions(catalogService);
    }

    @Test
    void getCatalogItemById_found_returnsItem() throws Exception {
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 42, "Pocky", 10));

        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Pocky"));
    }

    @Test
    void getCatalogItemById_missing_returnsNotFound() throws Exception {
        when(catalogService.getCatalogItemById(1)).thenThrow(new RuntimeException("Catalog item not found"));

        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Catalog item not found"));
    }

    @Test
    void updateCatalogItem_success_whenOwner_returnsUpdatedItem() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 42, "Pocky", 10));
        when(catalogService.updateCatalogItem(eq(1), any(CatalogItemUpdateRequest.class)))
                .thenReturn(item(1, 42, "Pocky", 20));

        mockMvc.perform(patch("/api/catalog/1")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated\",\"price\":250000,\"stock\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(20));

        verify(catalogService).updateCatalogItem(eq(1), any(CatalogItemUpdateRequest.class));
    }

    @Test
    void updateCatalogItem_forbidden_whenNotOwner_returnsForbidden() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 99, "Pocky", 10));

        mockMvc.perform(patch("/api/catalog/1")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated\",\"price\":250000,\"stock\":20}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only modify your own catalog items"));

        verify(catalogService).getCatalogItemById(1);
    }

    @Test
    void updateCatalogItem_admin_canUpdateAnyItem() throws Exception {
        User currentUser = user(1L, "admin@example.com", Role.ADMIN, AccountStatus.ACTIVE);
        when(authService.getByEmail("admin@example.com")).thenReturn(currentUser);
        when(catalogService.updateCatalogItem(eq(1), any(CatalogItemUpdateRequest.class)))
                .thenReturn(item(1, 99, "Pocky", 20));

        mockMvc.perform(patch("/api/catalog/1")
                        .principal(auth("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void updateCatalogItem_missing_returnsNotFound() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 42, "Pocky", 10));
        when(catalogService.updateCatalogItem(eq(1), any(CatalogItemUpdateRequest.class)))
                .thenThrow(new RuntimeException("Catalog item not found"));

        mockMvc.perform(patch("/api/catalog/1")
                        .principal(auth("jastiper@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":20}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Catalog item not found"));
    }

    @Test
    void deleteCatalogItem_success_whenOwner_returnsMessage() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 42, "Pocky", 10));
        doNothing().when(catalogService).deleteCatalogItem(1);

        mockMvc.perform(delete("/api/catalog/1").principal(auth("jastiper@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Catalog item deleted successfully"));
    }

    @Test
    void deleteCatalogItem_forbidden_whenNotOwner_returnsForbidden() throws Exception {
        User currentUser = user(42L, "jastiper@example.com", Role.JASTIPER, AccountStatus.ACTIVE);
        when(authService.getByEmail("jastiper@example.com")).thenReturn(currentUser);
        when(catalogService.getCatalogItemById(1)).thenReturn(item(1, 99, "Pocky", 10));

        mockMvc.perform(delete("/api/catalog/1").principal(auth("jastiper@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only modify your own catalog items"));

        verify(catalogService).getCatalogItemById(1);
    }

    @Test
    void deleteCatalogItem_admin_canDeleteAnyItem() throws Exception {
        User currentUser = user(1L, "admin@example.com", Role.ADMIN, AccountStatus.ACTIVE);
        when(authService.getByEmail("admin@example.com")).thenReturn(currentUser);
        doNothing().when(catalogService).deleteCatalogItem(1);

        mockMvc.perform(delete("/api/catalog/1").principal(auth("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Catalog item deleted successfully"));
    }

    @Test
    void reserveStock_success_returnsUpdatedItem() throws Exception {
        when(catalogService.reserveStock(1, 3)).thenReturn(item(1, 42, "Pocky", 7));

        mockMvc.perform(post("/api/catalog/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));
    }

    @Test
    void reserveStock_invalidQuantity_returnsBadRequest() throws Exception {
        when(catalogService.reserveStock(1, 0)).thenThrow(new IllegalArgumentException("Quantity must be greater than 0"));

        mockMvc.perform(post("/api/catalog/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Quantity must be greater than 0"));
    }

    @Test
    void reserveStock_insufficientStock_returnsBadRequest() throws Exception {
        when(catalogService.reserveStock(1, 99)).thenThrow(new IllegalArgumentException("Insufficient stock"));

        mockMvc.perform(post("/api/catalog/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient stock"));
    }

    @Test
    void reserveStock_missingItem_returnsNotFound() throws Exception {
        when(catalogService.reserveStock(1, 3)).thenThrow(new RuntimeException("Catalog item not found"));

        mockMvc.perform(post("/api/catalog/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Catalog item not found"));
    }

    @Test
    void releaseStock_success_returnsUpdatedItem() throws Exception {
        when(catalogService.releaseStock(1, 3)).thenReturn(item(1, 42, "Pocky", 13));

        mockMvc.perform(post("/api/catalog/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(13));
    }

    @Test
    void releaseStock_invalidQuantity_returnsBadRequest() throws Exception {
        when(catalogService.releaseStock(1, 0)).thenThrow(new IllegalArgumentException("Quantity must be greater than 0"));

        mockMvc.perform(post("/api/catalog/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Quantity must be greater than 0"));
    }

    @Test
    void releaseStock_missingItem_returnsNotFound() throws Exception {
        when(catalogService.releaseStock(1, 3)).thenThrow(new RuntimeException("Catalog item not found"));

        mockMvc.perform(post("/api/catalog/1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Catalog item not found"));
    }

    @Test
    void getAllCatalogItemsForAdmin_returnsArray() throws Exception {
        when(catalogService.getAllCatalogItems()).thenReturn(List.of(item(1, 42, "Pocky", 10)));

        mockMvc.perform(get("/api/admin/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pocky"));
    }

    @Test
    void adminDeleteCatalogItem_success_returnsMessage() throws Exception {
        doNothing().when(catalogService).deleteCatalogItem(1);

        mockMvc.perform(delete("/api/admin/catalog/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Catalog item deleted successfully by admin"));
    }

    @Test
    void adminDeleteCatalogItem_missing_returnsNotFound() throws Exception {
        doThrow(new RuntimeException("Catalog item not found")).when(catalogService).deleteCatalogItem(1);

        mockMvc.perform(delete("/api/admin/catalog/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Catalog item not found"));
    }
}
