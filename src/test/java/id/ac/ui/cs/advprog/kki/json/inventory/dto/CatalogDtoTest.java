package id.ac.ui.cs.advprog.kki.json.inventory.dto;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogDtoTest {

    @Test
    void catalogItemRequest_settersAndGetters_work() {
        CatalogItemRequest request = new CatalogItemRequest();

        request.setJastiperId(11);
        request.setName("Pocky");
        request.setDescription("Snack");
        request.setPrice(12000);
        request.setStock(5);
        request.setOrigin("Japan");
        request.setPurchaseDate("2026-05-01");

        assertEquals(11, request.getJastiperId());
        assertEquals("Pocky", request.getName());
        assertEquals("Snack", request.getDescription());
        assertEquals(12000, request.getPrice());
        assertEquals(5, request.getStock());
        assertEquals("Japan", request.getOrigin());
        assertEquals("2026-05-01", request.getPurchaseDate());
    }

    @Test
    void catalogItemUpdateRequest_settersAndGetters_work() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();

        request.setDescription("Updated");
        request.setPrice(15000);
        request.setStock(8);

        assertEquals("Updated", request.getDescription());
        assertEquals(15000, request.getPrice());
        assertEquals(8, request.getStock());
    }

    @Test
    void catalogReserveRequest_setterAndGetter_work() {
        CatalogReserveRequest request = new CatalogReserveRequest();
        request.setQuantity(3);
        assertEquals(3, request.getQuantity());
    }

    @Test
    void catalogItemResponse_mapsEntityFields() {
        CatalogItem item = sampleItem();
        CatalogItemResponse response = new CatalogItemResponse(item);

        assertEquals(1, response.getId());
        assertEquals(99, response.getJastiperId());
        assertEquals("Pocky", response.getName());
        assertEquals("Snack", response.getDescription());
        assertEquals(12000, response.getPrice());
        assertEquals(5, response.getStock());
        assertEquals("Japan", response.getOrigin());
        assertEquals("2026-05-01", response.getPurchaseDate());
        assertEquals("2026-05-10T12:00:00Z", response.getCreatedAt());
        assertEquals("2026-05-11T12:00:00Z", response.getUpdatedAt());
    }

    @Test
    void catalogItemIntegrationResponse_mapsEntityFields() {
        CatalogItem item = sampleItem();
        CatalogItemIntegrationResponse response = new CatalogItemIntegrationResponse(item);

        assertEquals(12000, response.getPrice());
        assertEquals(5, response.getStock());
        assertEquals(99, response.getJastiperId());
    }

    private CatalogItem sampleItem() {
        CatalogItem item = new CatalogItem();
        item.setJastiperId(99);
        item.setName("Pocky");
        item.setDescription("Snack");
        item.setPrice(12000);
        item.setStock(5);
        item.setOrigin("Japan");
        item.setPurchaseDate("2026-05-01");
        setField(item, "createdAt", "2026-05-10T12:00:00Z");
        setField(item, "updatedAt", "2026-05-11T12:00:00Z");
        setField(item, "id", 1);
        return item;
    }

    private void setField(CatalogItem item, String fieldName, Object value) {
        try {
            Field field = CatalogItem.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (value instanceof Integer intValue) {
                field.setInt(item, intValue);
            } else {
                field.set(item, value);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
