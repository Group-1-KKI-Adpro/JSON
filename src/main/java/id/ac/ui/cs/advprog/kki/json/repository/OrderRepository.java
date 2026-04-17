package id.ac.ui.cs.advprog.kki.json.repository;

import id.ac.ui.cs.advprog.kki.json.model.Order;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Repository
public class OrderRepository {

    private final List<Order> orders = new ArrayList<>();

    public Order save(Order order) {

        if (order.getId() == null) {
            order.setId(UUID.randomUUID().toString());
        }

        orders.add(order);
        return order;
    }

    public List<Order> findByBuyerId(String buyerId) {
        return orders.stream()
                .filter(order -> order.getBuyerId().equals(buyerId))
                .toList();
    }

    public List<Order> findByJastiperId(String jastiperId) {
        return orders.stream()
                .filter(order -> order.getJastiperId().equals(jastiperId))
                .toList();
    }
}