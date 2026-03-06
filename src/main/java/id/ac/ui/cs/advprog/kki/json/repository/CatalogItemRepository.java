package id.ac.ui.cs.advprog.kki.json.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import id.ac.ui.cs.advprog.kki.json.model.CatalogItem;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Integer> {

}