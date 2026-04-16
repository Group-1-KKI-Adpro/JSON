package id.ac.ui.cs.advprog.kki.json.controller;

import id.ac.ui.cs.advprog.kki.json.order.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.kki.json.model.Order;
import id.ac.ui.cs.advprog.kki.json.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.service.OrderService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(Authentication authentication,
                             @RequestBody CreateOrderRequest request) {

        String buyerId = authentication.getName();

        return orderService.createOrder(
                buyerId,
                request.getShippingAddress(),
                request.getItems(),
                request.getVoucherCode()
        );
    }

    @GetMapping("/me")
    public List<Order> getBuyerOrders(Authentication authentication) {
        String buyerId = authentication.getName();
        return orderService.getBuyerOrders(buyerId);
    }

    @GetMapping("/jastiper/me")
    public List<Order> getJastiperOrders(Authentication authentication) {
        String jastiperId = authentication.getName();
        return orderService.getJastiperOrders(jastiperId);
    }

    // ✅ FIXED HERE
    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id,
                              @RequestParam OrderStatus status) {
        return orderService.updateStatus(id, status);
    }

    @PostMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable String id) {
        return orderService.cancelOrder(id);
    }
}