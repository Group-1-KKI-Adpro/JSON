package id.ac.ui.cs.advprog.kki.json.inventory.dto;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogDtoTest {

    @Test
    void catalogItemRequest_gettersAndSetters_work() {
        CatalogItemRequest request = new CatalogItemRequest();

        request.setJastiperId(88);
        request.setName("Pocky");
        request.setDescription("Chocolate biscuit");
        request.setPrice(12000);
        request.setStock(20);
        request.setOrigin("Japan");
        request.setPurchaseDate("2026-05-01");

        assertEquals(88, request.getJastiperId());
        assertEquals("Pocky", request.getName());
        assertEquals("Chocolate biscuit", request.getDescription());
        assertEquals(12000, request.getPrice());
        assertEquals(20, request.getStock());
        assertEquals("Japan", request.getOrigin());
        assertEquals("2026-05-01", request.getPurchaseDate());
    }

    @Test
    void catalogItemUpdateRequest_gettersAndSetters_work() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();

        request.setDescription("Updated");
        request.setPrice(25000);
        request.setStock(12);

        assertEquals("Updated", request.getDescription());
        assertEquals(25000, request.getPrice());
        assertEquals(12, request.getStock());
    }

    @Test
    void catalogReserveRequest_gettersAndSetters_work() {
        CatalogReserveRequest request = new CatalogReserveRequest();

        request.setQuantity(3);

        assertEquals(3, request.getQuantity());
    }

    @Test
    void catalogItemResponse_mapsAllFieldsFromEntity() {
        CatalogItem item = sampleItem();
        CatalogItemResponse response = new CatalogItemResponse(item);

        assertEquals(1, response.getId());
        assertEquals(42, response.getJastiperId());
        assertEquals("Pocky", response.getName());
        assertEquals("Chocolate biscuit", response.getDescription());
        assertEquals(12000, response.getPrice());
        assertEquals(20, response.getStock());
        assertEquals("Japan", response.getOrigin());
        assertEquals("2026-05-01", response.getPurchaseDate());
        assertEquals("2026-05-02T10:00:00Z", response.getCreatedAt());
        assertEquals("2026-05-02T10:05:00Z", response.getUpdatedAt());
    }

    @Test
    void catalogItemIntegrationResponse_mapsPriceStockAndOwner() {
        CatalogItem item = sampleItem();
        CatalogItemIntegrationResponse response = new CatalogItemIntegrationResponse(item);

        assertEquals(12000, response.getPrice());
        assertEquals(20, response.getStock());
        assertEquals(42, response.getJastiperId());
    }

    private CatalogItem sampleItem() {
        CatalogItem item = new CatalogItem();
        item.setJastiperId(42);
        item.setName("Pocky");
        item.setDescription("Chocolate biscuit");
        item.setPrice(12000);
        item.setStock(20);
        item.setOrigin("Japan");
        item.setPurchaseDate("2026-05-01");
        try {
            java.lang.reflect.Field idField = CatalogItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.setInt(item, 1);

            java.lang.reflect.Field createdAtField = CatalogItem.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(item, "2026-05-02T10:00:00Z");

            java.lang.reflect.Field updatedAtField = CatalogItem.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(item, "2026-05-02T10:05:00Z");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return item;
    }
}
