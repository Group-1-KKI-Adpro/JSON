package id.ac.ui.cs.advprog.kki.json.controller;

import id.ac.ui.cs.advprog.kki.json.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemResponse;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.model.CatalogReserveRequest;
import id.ac.ui.cs.advprog.kki.json.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/catalog")
    public ResponseEntity<?> createCatalogItem(@RequestBody CatalogItemRequest request) {
        try {
            CatalogItem item = catalogService.createCatalogItem(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new CatalogItemResponse(item));
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
    public ResponseEntity<?> updateCatalogItem(@PathVariable int id,
                                               @RequestBody CatalogItemUpdateRequest request) {
        try {
            CatalogItem item = catalogService.updateCatalogItem(id, request);
            return ResponseEntity.ok(new CatalogItemResponse(item));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/catalog/{id}")
    public ResponseEntity<?> deleteCatalogItem(@PathVariable int id) {
        try {
            catalogService.deleteCatalogItem(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Catalog item deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/catalog/{id}/reserve")
    public ResponseEntity<?> reserveStock(@PathVariable int id,
                                          @RequestBody CatalogReserveRequest request) {
        try {
            CatalogItem item = catalogService.reserveStock(id, request.getQuantity());
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

    private Map<String, String> error(String message) {
        return Collections.singletonMap("error", message);
    }
}