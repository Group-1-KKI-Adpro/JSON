package id.ac.ui.cs.advprog.kki.json.inventory.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemResponse;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogReserveRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.inventory.service.CatalogService;
import id.ac.ui.cs.advprog.kki.json.model.AccountStatus;
import id.ac.ui.cs.advprog.kki.json.model.Role;
import id.ac.ui.cs.advprog.kki.json.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;
    private final AuthService authService;

    public CatalogController(CatalogService catalogService, AuthService authService) {
        this.catalogService = catalogService;
        this.authService = authService;
    }

    @PostMapping("/catalog")
    public ResponseEntity<?> createCatalogItem(
            @RequestBody CatalogItemRequest request,
            Authentication authentication
    ) {
        try {
            User currentUser = requireApprovedJastiper(authentication);

            CatalogItem item = catalogService.createCatalogItemForJastiper(
                    request,
                    Math.toIntExact(currentUser.getId())
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new CatalogItemResponse(item));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(error(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<CatalogItemResponse>> getAllCatalogItems() {
        List<CatalogItemResponse> items = catalogService.getAllCatalogItems()
                .stream()
                .map(CatalogItemResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @GetMapping("/catalog/search")
    public ResponseEntity<?> searchCatalogItems(@RequestParam(name = "keyword", required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().body(error("keyword is required"));
        }

        List<CatalogItemResponse> items = catalogService.searchCatalogItems(keyword)
                .stream()
                .map(CatalogItemResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @GetMapping("/catalog/jastiper/{jastiperId}")
    public ResponseEntity<List<CatalogItemResponse>> getCatalogItemsByJastiperId(@PathVariable int jastiperId) {
        List<CatalogItemResponse> items = catalogService.getCatalogItemsByJastiperId(jastiperId)
                .stream()
                .map(CatalogItemResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @GetMapping("/catalog/mine")
    public ResponseEntity<?> getMyCatalogItems(Authentication authentication) {
        try {
            User currentUser = requireApprovedJastiper(authentication);

            List<CatalogItemResponse> items = catalogService.getCatalogItemsByJastiperId(
                            Math.toIntExact(currentUser.getId()))
                    .stream()
                    .map(CatalogItemResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(items);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(error(e.getReason()));
        }
    }

    @GetMapping("/catalog/{id}")
    public ResponseEntity<?> getCatalogItemById(@PathVariable int id) {
        try {
            CatalogItem item = catalogService.getCatalogItemById(id);
            return ResponseEntity.ok(new CatalogItemResponse(item));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PatchMapping("/catalog/{id}")
    public ResponseEntity<?> updateCatalogItem(
            @PathVariable int id,
            @RequestBody CatalogItemUpdateRequest request,
            Authentication authentication
    ) {
        try {
            requireOwnerOrAdmin(authentication, id);
            CatalogItem item = catalogService.updateCatalogItem(id, request);
            return ResponseEntity.ok(new CatalogItemResponse(item));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(error(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/catalog/{id}")
    public ResponseEntity<?> deleteCatalogItem(@PathVariable int id, Authentication authentication) {
        try {
            requireOwnerOrAdmin(authentication, id);
            catalogService.deleteCatalogItem(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Catalog item deleted successfully"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(error(e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/catalog/{id}/reserve")
    public ResponseEntity<?> reserveStock(
            @PathVariable int id,
            @RequestBody CatalogReserveRequest request
    ) {
        try {
            CatalogItem item = catalogService.reserveStock(id, request.getQuantity());
            return ResponseEntity.ok(new CatalogItemResponse(item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/catalog/{id}/release")
    public ResponseEntity<?> releaseStock(
            @PathVariable int id,
            @RequestBody CatalogReserveRequest request
    ) {
        try {
            CatalogItem item = catalogService.releaseStock(id, request.getQuantity());
            return ResponseEntity.ok(new CatalogItemResponse(item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @GetMapping("/admin/catalog")
    public ResponseEntity<List<CatalogItemResponse>> getAllCatalogItemsForAdmin() {
        List<CatalogItemResponse> items = catalogService.getAllCatalogItems()
                .stream()
                .map(CatalogItemResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/admin/catalog/{id}")
    public ResponseEntity<?> adminDeleteCatalogItem(@PathVariable int id) {
        try {
            catalogService.deleteCatalogItem(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Catalog item deleted successfully by admin"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    private User requireApprovedJastiper(Authentication authentication) {
        User user = requireActiveUser(authentication);

        if (user.getRole() != Role.JASTIPER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only approved Jastipers can create catalog items"
            );
        }

        return user;
    }

    private User requireOwnerOrAdmin(Authentication authentication, int catalogItemId) {
        User user = requireActiveUser(authentication);

        if (user.getRole() == Role.ADMIN) {
            return user;
        }

        if (user.getRole() != Role.JASTIPER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only approved Jastipers can modify catalog items"
            );
        }

        CatalogItem item = catalogService.getCatalogItemById(catalogItemId);

        if (item.getJastiperId() != Math.toIntExact(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only modify your own catalog items"
            );
        }

        return user;
    }

    private User requireActiveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        User user = authService.getByEmail(authentication.getName());

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only active users can access catalog management"
            );
        }

        return user;
    }

    private Map<String, String> error(String message) {
        return Collections.singletonMap("error", message == null ? "Request failed" : message);
    }
}
