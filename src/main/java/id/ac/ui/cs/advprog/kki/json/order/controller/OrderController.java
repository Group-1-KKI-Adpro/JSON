package id.ac.ui.cs.advprog.kki.json.order.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.order.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.kki.json.order.dto.RatingRequest;
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

    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @PostMapping
    public Order createOrder(Authentication authentication,
                             @RequestHeader("Authorization") String authHeader,
                             @RequestBody CreateOrderRequest request) {
        User user = getAuthenticatedUser(authentication);
        String token = authHeader.replace("Bearer ", "");

        return orderService.createOrder(
                user.getId(),
                request.getShippingAddress(),
                request.getItems(),
                request.getVoucherCode(),
                token
        );
    }

    @GetMapping("/me")
    public List<Order> getBuyerOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return orderService.getBuyerOrders(user.getId());
    }

    @GetMapping("/jastiper/me")
    public List<Order> getJastiperOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return orderService.getJastiperOrders(user.getId());
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(Authentication authentication,
                              @PathVariable String id,
                              @RequestParam OrderStatus status) {
        User user = getAuthenticatedUser(authentication);
        return orderService.updateStatus(id, status, user.getId());
    }

    @PostMapping("/{id}/cancel")
    public Order cancelOrder(Authentication authentication,
                             @PathVariable String id) {
        User user = getAuthenticatedUser(authentication);
        return orderService.cancelOrder(id, user.getId());
    }

    @PostMapping("/{id}/rating")
    public Order rateOrder(Authentication authentication,
                           @PathVariable String id,
                           @RequestBody RatingRequest request) {
        User user = getAuthenticatedUser(authentication);
        return orderService.rateOrder(id, user.getId(), request);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication is required");
        }

        String email = (String) authentication.getPrincipal();
        return authService.getByEmail(email);
    }
}