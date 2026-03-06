package id.ac.ui.cs.advprog.kki.json.controller;

import id.ac.ui.cs.advprog.kki.json.model.Order;
import id.ac.ui.cs.advprog.kki.json.service.OrderService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // Constructor Injection (Spring recommended)
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Create Order
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    // Get buyer orders
    @GetMapping("/me")
    public List<Order> getBuyerOrders(@RequestParam String buyerId) {
        return orderService.getBuyerOrders(buyerId);
    }

    // Get jastiper orders
    @GetMapping("/jastiper/me")
    public List<Order> getJastiperOrders(@RequestParam String jastiperId) {
        return orderService.getJastiperOrders(jastiperId);
    }
}