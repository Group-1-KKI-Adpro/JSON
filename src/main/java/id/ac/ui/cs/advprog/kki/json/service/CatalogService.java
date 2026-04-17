package id.ac.ui.cs.advprog.kki.json.service;

import id.ac.ui.cs.advprog.kki.json.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    public CatalogItem createCatalogItem(CatalogItemRequest request) {
        validateCreateRequest(request);

        CatalogItem item = new CatalogItem();
        item.setJastiperId(request.getJastiperId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        item.setOrigin(request.getOrigin());
        item.setPurchaseDate(request.getPurchaseDate());

        return catalogItemRepository.save(item);
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

    private void validateCreateRequest(CatalogItemRequest request) {
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
}