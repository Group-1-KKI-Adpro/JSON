package id.ac.ui.cs.advprog.kki.json.inventory.service;

import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.inventory.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    public CatalogItem createCatalogItemForJastiper(CatalogItemRequest request, int authenticatedJastiperId) {
        validateCreateRequest(request);

        CatalogItem item = new CatalogItem();
        item.setJastiperId(authenticatedJastiperId);
        item.setName(request.getName().trim());
        item.setDescription(cleanText(request.getDescription()));
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        item.setOrigin(cleanText(request.getOrigin()));
        item.setPurchaseDate(cleanText(request.getPurchaseDate()));

        return catalogItemRepository.save(item);
    }

    /*
     * Kept only for compatibility with older tests/internal code.
     * HTTP catalog creation must use createCatalogItemForJastiper(...)
     * so jastiperId comes from the authenticated JWT user, not from request body.
     */
    @Deprecated
    public CatalogItem createCatalogItem(CatalogItemRequest request) {
        return createCatalogItemForJastiper(request, request.getJastiperId());
    }

    public List<CatalogItem> getAllCatalogItems() {
        return catalogItemRepository.findAll();
    }

    public CatalogItem getCatalogItemById(int id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catalog item not found"));
    }

    public CatalogItem updateCatalogItem(int id, CatalogItemUpdateRequest request) {
        CatalogItem item = getCatalogItemById(id);

        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            item.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                throw new IllegalArgumentException("Stock cannot be negative");
            }
            item.setStock(request.getStock());
        }

        return catalogItemRepository.save(item);
    }

    public void deleteCatalogItem(int id) {
        CatalogItem item = getCatalogItemById(id);
        catalogItemRepository.delete(item);
    }

    public CatalogItem reserveStock(int id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        CatalogItem item = getCatalogItemById(id);

        if (item.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }

        item.setStock(item.getStock() - quantity);
        return catalogItemRepository.save(item);
    }

    public CatalogItem releaseStock(int id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        CatalogItem item = getCatalogItemById(id);
        item.setStock(item.getStock() + quantity);

        return catalogItemRepository.save(item);
    }

    private void validateCreateRequest(CatalogItemRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be empty");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (request.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (request.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }
}