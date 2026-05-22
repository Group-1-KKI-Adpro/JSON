package id.ac.ui.cs.advprog.kki.json.inventory.repository;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CatalogItemRepositoryTest {

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @Test
    void findByJastiperId_returnsOnlyMatchingOwnerItems() {
        CatalogItem ownerItem = savedItem(42, "Pocky", "Chocolate biscuit", "Japan");
        savedItem(99, "KitKat", "Wafer snack", "Korea");

        List<CatalogItem> results = catalogItemRepository.findByJastiperId(42);

        assertEquals(1, results.size());
        assertEquals(ownerItem.getId(), results.get(0).getId());
        assertEquals("Pocky", results.get(0).getName());
    }

    @Test
    void searchQuery_matchesNameDescriptionAndOrigin() {
        savedItem(42, "Pocky", "Chocolate biscuit", "Japan");
        CatalogItem originMatch = savedItem(99, "Random snack", "Something else", "Singapore");
        CatalogItem descriptionMatch = savedItem(77, "Another", "Best biscuit", "Thailand");

        List<CatalogItem> nameResults = catalogItemRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
                        "pock", "pock", "pock");
        assertEquals(1, nameResults.size());
        assertEquals("Pocky", nameResults.get(0).getName());

        List<CatalogItem> originResults = catalogItemRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
                        "singapore", "singapore", "singapore");
        assertEquals(1, originResults.size());
        assertEquals(originMatch.getId(), originResults.get(0).getId());

        List<CatalogItem> descriptionResults = catalogItemRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
                        "biscuit", "biscuit", "biscuit");
        assertEquals(2, descriptionResults.size());
        assertTrue(descriptionResults.stream().anyMatch(item -> item.getId() == descriptionMatch.getId()));
    }

    @Test
    void findByIdForUpdate_returnsSavedEntity() {
        CatalogItem saved = savedItem(42, "Pocky", "Chocolate biscuit", "Japan");

        Optional<CatalogItem> result = catalogItemRepository.findByIdForUpdate(saved.getId());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
        assertEquals("Pocky", result.get().getName());
    }

    private CatalogItem savedItem(int jastiperId, String name, String description, String origin) {
        CatalogItem item = new CatalogItem();
        item.setJastiperId(jastiperId);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(12000);
        item.setStock(20);
        item.setOrigin(origin);
        item.setPurchaseDate("2026-05-01");
        return catalogItemRepository.saveAndFlush(item);
    }
}
