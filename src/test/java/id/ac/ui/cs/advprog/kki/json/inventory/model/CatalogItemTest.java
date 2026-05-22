package id.ac.ui.cs.advprog.kki.json.inventory.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CatalogItemTest {

    @Test
    void settersAndGetters_work() {
        CatalogItem item = new CatalogItem();

        item.setJastiperId(88);
        item.setName("Pocky");
        item.setDescription("Snack");
        item.setPrice(12000);
        item.setStock(10);
        item.setOrigin("Japan");
        item.setPurchaseDate("2026-05-01");

        assertEquals(88, item.getJastiperId());
        assertEquals("Pocky", item.getName());
        assertEquals("Snack", item.getDescription());
        assertEquals(12000, item.getPrice());
        assertEquals(10, item.getStock());
        assertEquals("Japan", item.getOrigin());
        assertEquals("2026-05-01", item.getPurchaseDate());
    }

    @Test
    void negativePrice_throwsException() {
        CatalogItem item = new CatalogItem();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.setPrice(-1));
        assertEquals("Price cannot be negative", ex.getMessage());
    }

    @Test
    void negativeStock_throwsException() {
        CatalogItem item = new CatalogItem();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.setStock(-1));
        assertEquals("Stock cannot be negative", ex.getMessage());
    }

    @Test
    void onCreate_setsTimestamps() throws Exception {
        CatalogItem item = new CatalogItem();

        invokeLifecycle(item, "onCreate");

        assertNotNull(item.getCreatedAt());
        assertEquals(item.getCreatedAt(), item.getUpdatedAt());
    }

    @Test
    void onUpdate_changesUpdatedAt() throws Exception {
        CatalogItem item = new CatalogItem();
        setField(item, "updatedAt", "old-value");

        invokeLifecycle(item, "onUpdate");

        assertNotEquals("old-value", item.getUpdatedAt());
    }

    @Test
    void getVersion_returnsVersionField() throws Exception {
        CatalogItem item = new CatalogItem();
        setField(item, "version", 7L);
        assertEquals(7L, item.getVersion());
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
