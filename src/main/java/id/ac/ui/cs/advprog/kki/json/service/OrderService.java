package id.ac.ui.cs.advprog.kki.json.service;

import id.ac.ui.cs.advprog.kki.json.model.*;
import id.ac.ui.cs.advprog.kki.json.repository.OrderRepository;
import id.ac.ui.cs.advprog.kki.json.order.dto.ItemRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;


import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final WalletService walletService;
    private final VoucherService voucherService;

    public OrderService(OrderRepository orderRepository,
                        InventoryService inventoryService,
                        WalletService walletService,
                        VoucherService voucherService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.walletService = walletService;
        this.voucherService = voucherService;
    }

    // 🔥 CREATE ORDER (fixed)
    public Order createOrder(String buyerId,
                             String shippingAddress,
                             List<ItemRequest> items,
                             String voucherCode) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = new Order();


        order.setBuyerId(buyerId);
        order.setShippingAddress(shippingAddress);

        double total = 0;

        for (ItemRequest req : items) {

            // ✅ VALIDATE BEFORE ENTITY CREATION
            if (req.getCatalogItemId() == null || req.getCatalogItemId().isBlank()) {
                throw new IllegalArgumentException("catalogItemId is required");
            }

            if (req.getQty() == null || req.getQty() <= 0) {
                throw new IllegalArgumentException("qty must be > 0");
            }

            if (req.getPriceSnapshot() == null || req.getPriceSnapshot() < 0) {
                throw new IllegalArgumentException("priceSnapshot must be >= 0");
            }

            // ✅ SAFE ENTITY CREATION
            OrderItem item = new OrderItem();
            item.setCatalogItemId(req.getCatalogItemId());
            item.setQty(req.getQty());
            item.setPriceSnapshot(req.getPriceSnapshot());

            order.addItem(item);

            total += req.getPriceSnapshot() * req.getQty();
        }

        // ✅ Inventory
        if (!inventoryService.checkStock(order.getItems())) {
            throw new RuntimeException("Stock not available");
        }
        inventoryService.reserveStock(order.getItems());

        // ✅ Voucher
        if (voucherCode != null && !voucherCode.isBlank()) {
            total = voucherService.applyDiscount(voucherCode, total);
        }

        // ✅ Wallet
        if (!walletService.hasEnoughBalance(buyerId, total)) {
            throw new RuntimeException("Insufficient balance");
        }

        walletService.deduct(buyerId, total);

        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherService.markUsed(voucherCode, buyerId);
        }

        order.setTotalPrice(total);
        order.setStatus(OrderStatus.PAID);

        return orderRepository.save(order);
    }

    // 🔄 STATUS UPDATE
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

    // ❌ CANCEL
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed order");
        }

        walletService.refund(order.getBuyerId(), order.getTotalPrice());

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    public List<Order> getBuyerOrders(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    public List<Order> getJastiperOrders(String jastiperId) {
        return orderRepository.findByJastiperId(jastiperId);
    }
}