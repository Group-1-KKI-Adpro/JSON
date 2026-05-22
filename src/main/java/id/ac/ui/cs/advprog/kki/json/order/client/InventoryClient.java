package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.inventory.service.CatalogService;
import id.ac.ui.cs.advprog.kki.json.order.dto.CatalogItemSnapshot;
import org.springframework.stereotype.Component;

@Component
public class InventoryClient {

    private final CatalogService catalogService;

    public InventoryClient(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public CatalogItemSnapshot getItem(int catalogId) {
        try {
            CatalogItem item = catalogService.getCatalogItemById(catalogId);
            return toSnapshot(item);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch catalog item " + catalogId);
        }
    }

    public void reserveItem(int catalogId, int quantity) {
        try {
            catalogService.reserveStock(catalogId, quantity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reserve stock for item " + catalogId);
        }
    }

    public void releaseItem(int catalogId, int quantity) {
        try {
            catalogService.releaseStock(catalogId, quantity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to release stock for item " + catalogId);
        }
    }

    private CatalogItemSnapshot toSnapshot(CatalogItem item) {
        CatalogItemSnapshot snapshot = new CatalogItemSnapshot();

        snapshot.setId(item.getId());
        snapshot.setJastiperId(item.getJastiperId());
        snapshot.setName(item.getName());
        snapshot.setDescription(item.getDescription());
        snapshot.setPrice(item.getPrice());
        snapshot.setStock(item.getStock());
        snapshot.setOrigin(item.getOrigin());
        snapshot.setPurchaseDate(item.getPurchaseDate());
        snapshot.setCreatedAt(item.getCreatedAt());
        snapshot.setUpdatedAt(item.getUpdatedAt());

        return snapshot;
    }
}