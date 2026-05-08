package id.ac.ui.cs.advprog.kki.json.order.repository;

import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {


    List<Order> findByBuyerId(Long buyerId);
    List<Order> findByJastiperId(Long jastiperId);
}