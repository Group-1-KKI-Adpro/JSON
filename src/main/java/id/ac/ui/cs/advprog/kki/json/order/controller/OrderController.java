package id.ac.ui.cs.advprog.kki.json.order.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.order.dto.CreateOrderRequest;
import id.ac.ui.cs.advprog.kki.json.order.dto.RatingRequest;
import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.order.service.OrderService;
import id.ac.ui.cs.advprog.kki.json.order.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;
    private final CartService cartService;

    public OrderController(OrderService orderService, AuthService authService, CartService cartService) {
        this.orderService = orderService;
        this.authService = authService;
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateOrderRequest request
    ) {
        try {
            User user = getAuthenticatedUser(authentication);
            String token = authHeader.replace("Bearer ", "");

            Order order = orderService.createOrder(
                    user.getId(),
                    request.getShippingAddress(),
                    request.getItems(),
                    request.getVoucherCode(),
                    token
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(error(e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getBuyerOrders(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            List<Order> orders = orderService.getBuyerOrders(user.getId());
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/jastiper/me")
    public ResponseEntity<?> getJastiperOrders(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            List<Order> orders = orderService.getJastiperOrders(user.getId());
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam OrderStatus status
    ) {
        try {
            User user = getAuthenticatedUser(authentication);
            Order order = orderService.updateStatus(id, status, user.getId());
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            Authentication authentication,
            @PathVariable String id
    ) {
        try {
            User user = getAuthenticatedUser(authentication);
            Order order = orderService.cancelOrder(id, user.getId());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/rating")
    public ResponseEntity<?> rateOrder(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody RatingRequest request
    ) {
        try {
            User user = getAuthenticatedUser(authentication);
            Order order = orderService.rateOrder(id, user.getId(), request);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        String email = (String) authentication.getPrincipal();
        return authService.getByEmail(email);
    }

    private Map<String, String> error(String message) {
        return Collections.singletonMap(
                "error",
                message == null || message.isBlank() ? "Request failed" : message
        );
    }

    @PostMapping("/cart")
    public ResponseEntity<?> addToCart(
            Authentication authentication,
            @RequestParam Long catalogItemId,
            @RequestParam Integer quantity
    ) {
        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                cartService.addToCart(
                        user.getId(),
                        catalogItemId,
                        quantity
                )
        );
    }

    @GetMapping("/cart")
    public ResponseEntity<?> getCart(
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                cartService.getCart(user.getId())
        );
    }

}