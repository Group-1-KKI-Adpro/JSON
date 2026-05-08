package id.ac.ui.cs.advprog.kki.json.inventory.repository;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Integer> {
    List<CatalogItem> findByJastiperId(int jastiperId);
}
