package id.ac.ui.cs.advprog.kki.json.inventory.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CatalogItemTest {

    @Test
    void settersAndGetters_workAsExpected() {
        CatalogItem item = new CatalogItem();

        item.setJastiperId(77);
        item.setName("Pocky");
        item.setDescription("Chocolate biscuit");
        item.setPrice(12000);
        item.setStock(20);
        item.setOrigin("Japan");
        item.setPurchaseDate("2026-05-01");

        assertEquals(77, item.getJastiperId());
        assertEquals("Pocky", item.getName());
        assertEquals("Chocolate biscuit", item.getDescription());
        assertEquals(12000, item.getPrice());
        assertEquals(20, item.getStock());
        assertEquals("Japan", item.getOrigin());
        assertEquals("2026-05-01", item.getPurchaseDate());
        assertNull(item.getCreatedAt());
        assertNull(item.getUpdatedAt());
        assertNull(item.getVersion());
    }

    @Test
    void setPrice_negative_throwsException() {
        CatalogItem item = new CatalogItem();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.setPrice(-1));
        assertEquals("Price cannot be negative", ex.getMessage());
    }

    @Test
    void setStock_negative_throwsException() {
        CatalogItem item = new CatalogItem();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.setStock(-1));
        assertEquals("Stock cannot be negative", ex.getMessage());
    }

    @Test
    void onCreate_setsCreatedAndUpdatedAtTogether() throws Exception {
        CatalogItem item = new CatalogItem();

        invokeLifecycle(item, "onCreate");

        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
        assertEquals(item.getCreatedAt(), item.getUpdatedAt());
    }

    @Test
    void onUpdate_changesUpdatedAtAndKeepsCreatedAt() throws Exception {
        CatalogItem item = new CatalogItem();

        setField(item, "createdAt", "2026-05-01T00:00:00Z");
        setField(item, "updatedAt", "2026-05-01T00:00:00Z");

        Thread.sleep(5L);
        invokeLifecycle(item, "onUpdate");

        assertEquals("2026-05-01T00:00:00Z", item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
        assertNotEquals("2026-05-01T00:00:00Z", item.getUpdatedAt());
    }

    private void invokeLifecycle(CatalogItem item, String methodName) throws Exception {
        Method method = CatalogItem.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(item);
    }

    private void setField(CatalogItem item, String fieldName, Object value) throws Exception {
        Field field = CatalogItem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(item, value);
    }
}
