package id.ac.ui.cs.advprog.kki.json.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import id.ac.ui.cs.advprog.kki.json.model.CatalogItem;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItemRequest;
import id.ac.ui.cs.advprog.kki.json.repository.CatalogItemRepository;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @PostMapping
    public CatalogItem createCatalogItem(@RequestBody CatalogItemRequest request) {

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

    @GetMapping
    public List<CatalogItem> getAllCatalogItems() {
        return catalogItemRepository.findAll();
    }
}