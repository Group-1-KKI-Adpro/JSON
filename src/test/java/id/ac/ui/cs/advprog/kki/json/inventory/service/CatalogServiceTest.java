package id.ac.ui.cs.advprog.kki.json.inventory.service;

import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.dto.CatalogItemUpdateRequest;
import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.inventory.repository.CatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @InjectMocks
    private CatalogService catalogService;

    private CatalogItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new CatalogItem();
        setId(sampleItem, 1);
        sampleItem.setJastiperId(99);
        sampleItem.setName("Pocky");
        sampleItem.setDescription("Chocolate biscuit");
        sampleItem.setPrice(12000);
        sampleItem.setStock(20);
        sampleItem.setOrigin("Japan");
        sampleItem.setPurchaseDate("2026-05-01");
    }

    @Test
    void createCatalogItem_success_savesMappedEntity() {
        CatalogItemRequest request = validCreateRequest();
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = catalogService.createCatalogItem(request);

        assertNotNull(result);
        assertEquals(99, result.getJastiperId());
        assertEquals("Pocky", result.getName());
        assertEquals("Chocolate biscuit", result.getDescription());
        assertEquals(12000, result.getPrice());
        assertEquals(20, result.getStock());
        assertEquals("Japan", result.getOrigin());
        assertEquals("2026-05-01", result.getPurchaseDate());

        ArgumentCaptor<CatalogItem> captor = ArgumentCaptor.forClass(CatalogItem.class);
        verify(catalogItemRepository).save(captor.capture());
        assertEquals("Pocky", captor.getValue().getName());
    }

    @Test
    void createCatalogItem_nullRequest_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.createCatalogItem(null));
        assertEquals("Request cannot be null", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void createCatalogItem_blankName_throwsException() {
        CatalogItemRequest request = validCreateRequest();
        request.setName(" ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.createCatalogItem(request));
        assertEquals("Name cannot be empty", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void createCatalogItem_negativePrice_throwsException() {
        CatalogItemRequest request = validCreateRequest();
        request.setPrice(-1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.createCatalogItem(request));
        assertEquals("Price cannot be negative", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void createCatalogItem_negativeStock_throwsException() {
        CatalogItemRequest request = validCreateRequest();
        request.setStock(-1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.createCatalogItem(request));
        assertEquals("Stock cannot be negative", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void getAllCatalogItems_returnsRepositoryResults() {
        when(catalogItemRepository.findAll()).thenReturn(List.of(sampleItem));

        List<CatalogItem> result = catalogService.getAllCatalogItems();

        assertEquals(1, result.size());
        assertEquals("Pocky", result.get(0).getName());
        verify(catalogItemRepository).findAll();
    }

    @Test
    void searchCatalogItems_nullKeyword_returnsAllItems() {
        when(catalogItemRepository.findAll()).thenReturn(List.of(sampleItem));

        List<CatalogItem> result = catalogService.searchCatalogItems(null);

        assertEquals(1, result.size());
        verify(catalogItemRepository).findAll();
        verify(catalogItemRepository, never())
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(anyString(), anyString(), anyString());
    }

    @Test
    void searchCatalogItems_blankKeyword_returnsAllItems() {
        when(catalogItemRepository.findAll()).thenReturn(List.of(sampleItem));

        List<CatalogItem> result = catalogService.searchCatalogItems("   ");

        assertEquals(1, result.size());
        verify(catalogItemRepository).findAll();
    }

    @Test
    void searchCatalogItems_trimmedKeyword_searchesRepository() {
        when(catalogItemRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
                "pocky", "pocky", "pocky")).thenReturn(List.of(sampleItem));

        List<CatalogItem> result = catalogService.searchCatalogItems("  pocky  ");

        assertEquals(1, result.size());
        verify(catalogItemRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
                "pocky", "pocky", "pocky");
    }

    @Test
    void getCatalogItemsByJastiperId_returnsRepositoryResults() {
        when(catalogItemRepository.findByJastiperId(99)).thenReturn(List.of(sampleItem));

        List<CatalogItem> result = catalogService.getCatalogItemsByJastiperId(99);

        assertEquals(1, result.size());
        verify(catalogItemRepository).findByJastiperId(99);
    }

    @Test
    void getCatalogItemById_found_returnsItem() {
        when(catalogItemRepository.findById(1)).thenReturn(Optional.of(sampleItem));

        CatalogItem result = catalogService.getCatalogItemById(1);

        assertEquals("Pocky", result.getName());
        verify(catalogItemRepository).findById(1);
    }

    @Test
    void getCatalogItemById_missing_throwsException() {
        when(catalogItemRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.getCatalogItemById(1));
        assertEquals("Catalog item not found", ex.getMessage());
        verify(catalogItemRepository).findById(1);
    }

    @Test
    void updateCatalogItem_success_updatesAllowedFields() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();
        request.setDescription("Updated description");
        request.setPrice(13000);
        request.setStock(15);

        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = catalogService.updateCatalogItem(1, request);

        assertEquals("Updated description", result.getDescription());
        assertEquals(13000, result.getPrice());
        assertEquals(15, result.getStock());
        verify(catalogItemRepository).findByIdForUpdate(1);
        verify(catalogItemRepository).save(sampleItem);
    }

    @Test
    void updateCatalogItem_descriptionOnly_keepsOtherFields() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();
        request.setDescription("New description");

        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = catalogService.updateCatalogItem(1, request);

        assertEquals("New description", result.getDescription());
        assertEquals(12000, result.getPrice());
        assertEquals(20, result.getStock());
    }

    @Test
    void updateCatalogItem_missing_throwsException() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.updateCatalogItem(1, new CatalogItemUpdateRequest()));
        assertEquals("Catalog item not found", ex.getMessage());
    }

    @Test
    void updateCatalogItem_negativePrice_throwsException() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();
        request.setPrice(-1);

        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.updateCatalogItem(1, request));
        assertEquals("Price cannot be negative", ex.getMessage());
        verify(catalogItemRepository, never()).save(any(CatalogItem.class));
    }

    @Test
    void updateCatalogItem_negativeStock_throwsException() {
        CatalogItemUpdateRequest request = new CatalogItemUpdateRequest();
        request.setStock(-1);

        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.updateCatalogItem(1, request));
        assertEquals("Stock cannot be negative", ex.getMessage());
        verify(catalogItemRepository, never()).save(any(CatalogItem.class));
    }

    @Test
    void deleteCatalogItem_success_deletesEntity() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));

        catalogService.deleteCatalogItem(1);

        verify(catalogItemRepository).delete(sampleItem);
    }

    @Test
    void deleteCatalogItem_missing_throwsException() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.deleteCatalogItem(1));
        assertEquals("Catalog item not found", ex.getMessage());
        verify(catalogItemRepository, never()).delete(any());
    }

    @Test
    void reserveStock_success_reducesStock() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = catalogService.reserveStock(1, 5);

        assertEquals(15, result.getStock());
        verify(catalogItemRepository).save(sampleItem);
    }

    @Test
    void reserveStock_zeroQuantity_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.reserveStock(1, 0));
        assertEquals("Quantity must be greater than 0", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void reserveStock_insufficientStock_throwsException() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.reserveStock(1, 999));
        assertEquals("Insufficient stock", ex.getMessage());
        verify(catalogItemRepository, never()).save(any(CatalogItem.class));
    }

    @Test
    void reserveStock_missingItem_throwsException() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.reserveStock(1, 5));
        assertEquals("Catalog item not found", ex.getMessage());
    }

    @Test
    void releaseStock_success_increasesStock() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleItem));
        when(catalogItemRepository.save(any(CatalogItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = catalogService.releaseStock(1, 5);

        assertEquals(25, result.getStock());
        verify(catalogItemRepository).save(sampleItem);
    }

    @Test
    void releaseStock_zeroQuantity_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> catalogService.releaseStock(1, 0));
        assertEquals("Quantity must be greater than 0", ex.getMessage());
        verifyNoInteractions(catalogItemRepository);
    }

    @Test
    void releaseStock_missingItem_throwsException() {
        when(catalogItemRepository.findByIdForUpdate(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> catalogService.releaseStock(1, 5));
        assertEquals("Catalog item not found", ex.getMessage());
    }

    private CatalogItemRequest validCreateRequest() {
        CatalogItemRequest request = new CatalogItemRequest();
        request.setJastiperId(99);
        request.setName("Pocky");
        request.setDescription("Chocolate biscuit");
        request.setPrice(12000);
        request.setStock(20);
        request.setOrigin("Japan");
        request.setPurchaseDate("2026-05-01");
        return request;
    }

    private void setId(CatalogItem item, int id) {
        try {
            Field field = CatalogItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.setInt(item, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
