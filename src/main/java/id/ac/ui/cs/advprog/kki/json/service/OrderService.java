package id.ac.ui.cs.advprog.kki.json.service;

import id.ac.ui.cs.advprog.kki.json.model.Order;
import id.ac.ui.cs.advprog.kki.json.repository.OrderRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(Order order) {
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public List<Order> getBuyerOrders(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> getJastiperOrders(String jastiperId) {
        return orderRepository.findByJastiperId(jastiperId);
    }
}