package id.ac.ui.cs.advprog.kki.json.order.service;

import id.ac.ui.cs.advprog.kki.json.auth.dto.UpdateReputationRequest;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.order.client.InventoryClient;
import id.ac.ui.cs.advprog.kki.json.order.client.VoucherClient;
import id.ac.ui.cs.advprog.kki.json.order.client.WalletClient;
import id.ac.ui.cs.advprog.kki.json.order.dto.CatalogItemSnapshot;
import id.ac.ui.cs.advprog.kki.json.order.dto.ItemRequest;
import id.ac.ui.cs.advprog.kki.json.order.model.Order;
import id.ac.ui.cs.advprog.kki.json.order.model.OrderStatus;
import id.ac.ui.cs.advprog.kki.json.order.repository.OrderRepository;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private WalletClient walletClient;

    @Mock
    private VoucherClient voucherClient;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderDeductsWalletUsingSavedOrderId() {
        ItemRequest itemRequest = itemRequest(101, 2);
        CatalogItemSnapshot snapshot = catalogItemSnapshot(101, 99, 250);

        when(inventoryClient.getItem(101)).thenReturn(snapshot);
        when(walletClient.getBalance("token-123")).thenReturn(1_000L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", "order-123");
            return order;
        });
        when(walletClient.deduct(
                7L,
                500L,
                "order-123",
                "Payment for order order-123"
        )).thenReturn(transactionResponse(1L, TransactionType.PAYMENT, 500L, "order-123"));

        Order savedOrder = orderService.createOrder(
                7L,
                "Jakarta",
                List.of(itemRequest),
                null,
                "token-123"
        );

        assertEquals("order-123", savedOrder.getId());
        assertEquals(500L, savedOrder.getTotalPrice());
        assertEquals(OrderStatus.PAID, savedOrder.getStatus());
        assertEquals(99L, savedOrder.getJastiperId());
        assertEquals(1, savedOrder.getItems().size());

        verify(inventoryClient).reserveItem(101, 2);
        verify(walletClient).deduct(
                7L,
                500L,
                "order-123",
                "Payment for order order-123"
        );
        verify(voucherClient, never()).useVoucher(any(), any(), any());
    }

    @Test
    void createOrderRefundsDeductionWhenLaterStepFails() {
        ItemRequest itemRequest = itemRequest(101, 1);
        CatalogItemSnapshot snapshot = catalogItemSnapshot(101, 99, 300);

        when(inventoryClient.getItem(101)).thenReturn(snapshot);
        when(walletClient.getBalance("token-123")).thenReturn(1_000L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", "order-rollback");
            return order;
        });
        when(walletClient.deduct(
                7L,
                150L,
                "order-rollback",
                "Payment for order order-rollback"
        )).thenReturn(transactionResponse(1L, TransactionType.PAYMENT, 150L, "order-rollback"));
        when(voucherClient.applyVoucher("PROMO", 300.0)).thenReturn(150.0);
        doNothing().when(inventoryClient).reserveItem(101, 1);
        doThrow(new RuntimeException("Voucher use failed"))
                .when(voucherClient).useVoucher("PROMO", "order-rollback", "token-123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.createOrder(
                7L,
                "Jakarta",
                List.of(itemRequest),
                "PROMO",
                "token-123"
        ));

        assertEquals("Voucher use failed", ex.getMessage());
        verify(walletClient).refund(
                7L,
                150L,
                "order-rollback",
                "Compensation refund for failed order order-rollback"
        );
        verify(inventoryClient).releaseItem(101, 1);
    }

    @Test
    void cancelOrderRefundsWalletAndMarksOrderCancelled() {
        Order existingOrder = new Order();
        ReflectionTestUtils.setField(existingOrder, "id", "order-9");
        existingOrder.setBuyerId(7L);
        existingOrder.setJastiperId(99L);
        existingOrder.setShippingAddress("Jakarta");
        existingOrder.setTotalPrice(450L);
        existingOrder.setStatus(OrderStatus.PAID);
        existingOrder.addItem(orderItem(101, 1, 450L));

        when(orderRepository.findByIdForUpdate("order-9")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(existingOrder);

        Order result = orderService.cancelOrder("order-9", 99L);

        assertSame(existingOrder, result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(inventoryClient).releaseItem(101, 1);
        verify(walletClient).refund(
                7L,
                450L,
                "order-9",
                "Refund for cancelled order order-9"
        );

        ArgumentCaptor<UpdateReputationRequest> reputationCaptor =
                ArgumentCaptor.forClass(UpdateReputationRequest.class);
        verify(authService).updateJastiperReputation(eq(99L), reputationCaptor.capture());
        assertTrue(Boolean.TRUE.equals(reputationCaptor.getValue().getTransactionCompleted()));
        assertTrue(Boolean.FALSE.equals(reputationCaptor.getValue().getTransactionSuccessful()));
    }

    private ItemRequest itemRequest(int catalogItemId, int qty) {
        ItemRequest request = new ItemRequest();
        request.setCatalogItemId(catalogItemId);
        request.setQty(qty);
        return request;
    }

    private CatalogItemSnapshot catalogItemSnapshot(int id, int jastiperId, int price) {
        CatalogItemSnapshot snapshot = new CatalogItemSnapshot();
        snapshot.setId(id);
        snapshot.setJastiperId(jastiperId);
        snapshot.setPrice(price);
        return snapshot;
    }

    private id.ac.ui.cs.advprog.kki.json.order.model.OrderItem orderItem(int catalogItemId,
                                                                          int qty,
                                                                          long priceSnapshot) {
        id.ac.ui.cs.advprog.kki.json.order.model.OrderItem item =
                new id.ac.ui.cs.advprog.kki.json.order.model.OrderItem();
        item.setCatalogItemId(catalogItemId);
        item.setQty(qty);
        item.setPriceSnapshot(priceSnapshot);
        return item;
    }

    private TransactionResponse transactionResponse(Long id,
                                                    TransactionType type,
                                                    long amount,
                                                    String referenceId) {
        return new TransactionResponse(
                id,
                7L,
                type,
                amount,
                TransactionStatus.SUCCESS,
                Instant.now(),
                null,
                referenceId,
                1_000L,
                500L,
                null
        );
    }
}
