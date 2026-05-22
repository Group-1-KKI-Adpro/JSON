package id.ac.ui.cs.advprog.kki.json.inventory.repository;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Integer> {
    List<CatalogItem> findByJastiperId(int jastiperId);

    List<CatalogItem> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOriginContainingIgnoreCase(
            String nameKeyword,
            String descriptionKeyword,
            String originKeyword
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CatalogItem c where c.id = :id")
    Optional<CatalogItem> findByIdForUpdate(@Param("id") int id);
}