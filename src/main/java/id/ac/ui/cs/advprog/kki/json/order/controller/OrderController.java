package id.ac.ui.cs.advprog.kki.json.order.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.order.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.order.service.OrderService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderController(OrderService orderService,
                           AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    // 🔥 CREATE ORDER (FIXED TYPE)
    @PostMapping
    public Order createOrder(Authentication authentication,
                             @RequestHeader("Authorization") String authHeader,
                             @RequestBody CreateOrderRequest request) {

        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);

        // ✅ FIX: use Long
        Long buyerId = user.getId();

        String token = authHeader.replace("Bearer ", "");

        return orderService.createOrder(
                buyerId,
                request.getShippingAddress(),
                request.getItems(),
                request.getVoucherCode(),
                token
        );
    }

    // 📦 BUYER ORDERS
    @GetMapping("/me")
    public List<Order> getBuyerOrders(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);

        return orderService.getBuyerOrders(user.getId());
    }

    // 🚚 JASTIPER ORDERS
    @GetMapping("/jastiper/me")
    public List<Order> getJastiperOrders(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);

        return orderService.getJastiperOrders(user.getId());
    }

    // 🔄 STATUS UPDATE
    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id,
                              @RequestParam OrderStatus status) {
        return orderService.updateStatus(id, status);
    }

    // ❌ CANCEL
    @PostMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable String id) {
        return orderService.cancelOrder(id);
    }
}
