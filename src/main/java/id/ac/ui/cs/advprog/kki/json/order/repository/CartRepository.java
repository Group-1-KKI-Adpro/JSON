package id.ac.ui.cs.advprog.kki.json.order.repository;

import id.ac.ui.cs.advprog.kki.json.order.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository
        extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);
}