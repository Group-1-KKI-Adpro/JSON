package id.ac.ui.cs.advprog.kki.json.repository;

import id.ac.ui.cs.advprog.kki.json.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByBuyerId(String buyerId);

    List<Order> findByJastiperId(String jastiperId);
}