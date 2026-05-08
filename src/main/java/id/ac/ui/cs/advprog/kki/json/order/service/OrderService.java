package id.ac.ui.cs.advprog.kki.json.order.service;

import id.ac.ui.cs.advprog.kki.json.order.dto.ItemRequest;
import org.springframework.stereotype.Service;
import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderItem;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.order.repository.OrderRepository;

import id.ac.ui.cs.advprog.kki.json.order.client.InventoryClient;
import id.ac.ui.cs.advprog.kki.json.order.client.WalletClient;
import id.ac.ui.cs.advprog.kki.json.order.client.VoucherClient;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final WalletClient walletClient;
    private final VoucherClient voucherClient;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        WalletClient walletClient,
                        VoucherClient voucherClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.walletClient = walletClient;
        this.voucherClient = voucherClient;
    }

    // 🔥 CREATE ORDER (FINAL FIXED)
    public Order createOrder(Long buyerId,
                             String shippingAddress,
                             List<ItemRequest> items,
                             String voucherCode,
                             String token) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setShippingAddress(shippingAddress);

        long total = 0;

        // ✅ Build order + validation
        for (ItemRequest req : items) {

            if (req.getCatalogItemId() == null) {
                throw new IllegalArgumentException("catalogItemId is required");
            }

            if (req.getQty() == null || req.getQty() <= 0) {
                throw new IllegalArgumentException("qty must be > 0");
            }

            if (req.getPriceSnapshot() == null || req.getPriceSnapshot() < 0) {
                throw new IllegalArgumentException("priceSnapshot must be >= 0");
            }

            OrderItem item = new OrderItem();
            item.setCatalogItemId(req.getCatalogItemId()); // Integer
            item.setQty(req.getQty());
            item.setPriceSnapshot(req.getPriceSnapshot().longValue()); // convert Double → Long

            order.addItem(item);

            total += req.getPriceSnapshot().longValue() * req.getQty();
        }

        if (voucherCode != null && !voucherCode.isBlank()) {
            double discounted = voucherClient.applyVoucher(voucherCode, (double) total);
            total = (long) discounted;
        }

        long balance = walletClient.getBalance(token);
        if (balance < total) {
            throw new RuntimeException("Insufficient balance");
        }

        for (OrderItem item : order.getItems()) {
            inventoryClient.reserveItem(
                    item.getCatalogItemId(),
                    item.getQty()
            );
        }

        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PAID);

        Order savedOrder = orderRepository.save(order);


        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherClient.useVoucher(voucherCode, savedOrder.getId(), token);
        }

        return savedOrder;
    }

    public Order updateStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!isValidTransition(order.getStatus(), newStatus)) {
            throw new RuntimeException("Invalid status transition");
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PAID -> next == OrderStatus.PURCHASED;
            case PURCHASED -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.COMPLETED;
            default -> false;
        };
    }


    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed order");
        }

        walletClient.refund(order.getBuyerId(), order.getTotalPrice());

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public List<Order> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> getJastiperOrders(Long jastiperId) {
        return orderRepository.findByJastiperId(jastiperId);
    }
}
