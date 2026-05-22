package id.ac.ui.cs.advprog.kki.json.order.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateReputationRequest;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.order.client.InventoryClient;
import id.ac.ui.cs.advprog.kki.json.order.client.WalletClient;
import id.ac.ui.cs.advprog.kki.json.order.client.VoucherClient;
import id.ac.ui.cs.advprog.kki.json.order.dto.CatalogItemSnapshot;
import id.ac.ui.cs.advprog.kki.json.order.dto.ItemRequest;
import id.ac.ui.cs.advprog.kki.json.order.dto.RatingRequest;
import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderItem;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final WalletClient walletClient;
    private final VoucherClient voucherClient;
    private final AuthService authService;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        WalletClient walletClient,
                        VoucherClient voucherClient,
                        AuthService authService) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.walletClient = walletClient;
        this.voucherClient = voucherClient;
        this.authService = authService;
    }

    @Transactional
    public Order createOrder(Long buyerId,
                             String shippingAddress,
                             List<ItemRequest> items,
                             String voucherCode,
                             String token) {

        if (buyerId == null) {
            throw new IllegalArgumentException("buyerId is required");
        }

        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setShippingAddress(shippingAddress.trim());

        Long orderJastiperId = null;
        long total = 0;
        List<OrderItem> reservedItems = new ArrayList<>();

        try {
            for (ItemRequest req : items) {
                if (req.getCatalogItemId() == null) {
                    throw new IllegalArgumentException("catalogItemId is required");
                }

                if (req.getQty() == null || req.getQty() <= 0) {
                    throw new IllegalArgumentException("qty must be > 0");
                }

                CatalogItemSnapshot catalogItem = inventoryClient.getItem(req.getCatalogItemId());

                if (catalogItem == null) {
                    throw new RuntimeException("Catalog item not found");
                }

                if (catalogItem.getJastiperId() == null) {
                    throw new RuntimeException("Catalog item has no Jastiper owner");
                }

                Long itemJastiperId = catalogItem.getJastiperId().longValue();

                if (orderJastiperId == null) {
                    orderJastiperId = itemJastiperId;
                } else if (!orderJastiperId.equals(itemJastiperId)) {
                    throw new IllegalArgumentException("One order can only contain items from one Jastiper");
                }

                if (catalogItem.getPrice() == null || catalogItem.getPrice() < 0) {
                    throw new RuntimeException("Invalid catalog item price");
                }

                OrderItem item = new OrderItem();
                item.setCatalogItemId(req.getCatalogItemId());
                item.setQty(req.getQty());
                item.setPriceSnapshot(catalogItem.getPrice().longValue());

                order.addItem(item);
                total += catalogItem.getPrice().longValue() * req.getQty();
            }

            if (orderJastiperId == null) {
                throw new RuntimeException("Cannot determine Jastiper for this order");
            }

            if (buyerId.equals(orderJastiperId)) {
                throw new IllegalArgumentException("Buyer cannot order their own catalog item");
            }

            order.setJastiperId(orderJastiperId);

            if (voucherCode != null && !voucherCode.isBlank()) {
                double discounted = voucherClient.applyVoucher(voucherCode, (double) total);
                total = (long) discounted;
            }

            long balance = walletClient.getBalance(token);
            if (balance < total) {
                throw new RuntimeException("Insufficient balance");
            }

            for (OrderItem item : order.getItems()) {
                inventoryClient.reserveItem(item.getCatalogItemId(), item.getQty());
                reservedItems.add(item);
            }

            order.setTotalPrice(total);
            order.setStatus(OrderStatus.PAID);

            Order savedOrder = orderRepository.save(order);

            if (voucherCode != null && !voucherCode.isBlank()) {
                voucherClient.useVoucher(voucherCode, savedOrder.getId(), token);
            }

            return savedOrder;
        } catch (RuntimeException e) {
            releaseReservedStockQuietly(reservedItems);
            throw e;
        }
    }

    @Transactional
    public Order updateStatus(String orderId, OrderStatus newStatus, Long actorUserId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (newStatus == null) {
            throw new IllegalArgumentException("New status is required");
        }

        if (!order.getJastiperId().equals(actorUserId)) {
            throw new RuntimeException("Only the Jastiper who owns this order can update the status");
        }

        if (!isValidTransition(order.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid status transition");
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (newStatus == OrderStatus.COMPLETED) {
            authService.updateJastiperReputation(
                    savedOrder.getJastiperId(),
                    new UpdateReputationRequest(true, true, null)
            );
        }

        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(String orderId, Long actorUserId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getJastiperId().equals(actorUserId)) {
            throw new RuntimeException("Only the Jastiper who owns this order can cancel it");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }

        releaseReservedStockQuietly(order.getItems());
        walletClient.refund(order.getBuyerId(), order.getTotalPrice());

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        authService.updateJastiperReputation(
                savedOrder.getJastiperId(),
                new UpdateReputationRequest(true, false, null)
        );

        return savedOrder;
    }

    @Transactional
    public Order rateOrder(String orderId, Long buyerId, RatingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Rating request is required");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("Only the buyer who made this order can rate it");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Only COMPLETED orders can be rated");
        }

        if (order.getJastiperRating() != null || order.getProductRating() != null) {
            throw new RuntimeException("Order has already been rated");
        }

        validateRating(request.getJastiperRating(), "Jastiper rating");
        validateRating(request.getProductRating(), "Product rating");

        String review = request.getReview();
        if (review != null && review.length() > 500) {
            throw new IllegalArgumentException("Review cannot exceed 500 characters");
        }

        order.setJastiperRating(request.getJastiperRating());
        order.setProductRating(request.getProductRating());
        order.setReview(review == null ? null : review.trim());

        Order savedOrder = orderRepository.save(order);

        authService.updateJastiperReputation(
                savedOrder.getJastiperId(),
                new UpdateReputationRequest(false, null, request.getJastiperRating())
        );

        return savedOrder;
    }

    private void releaseReservedStockQuietly(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (OrderItem item : items) {
            try {
                inventoryClient.releaseItem(item.getCatalogItemId(), item.getQty());
            } catch (Exception ignored) {
                // Best-effort rollback of remote inventory reservation.
            }
        }
    }

    private void validateRating(Integer rating, String fieldName) {
        if (rating == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5");
        }
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PAID -> next == OrderStatus.PURCHASED;
            case PURCHASED -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.COMPLETED;
            default -> false;
        };
    }

    public List<Order> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> getJastiperOrders(Long jastiperId) {
        return orderRepository.findByJastiperId(jastiperId);
    }
}