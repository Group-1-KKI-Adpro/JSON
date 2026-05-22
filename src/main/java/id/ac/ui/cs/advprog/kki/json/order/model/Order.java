package id.ac.ui.cs.advprog.kki.json.order.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Long buyerId;

    private Long jastiperId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false)
    private Long totalPrice;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private Integer jastiperRating;

    @Column
    private Integer productRating;

    @Column(length = 500)
    private String review;

    @JsonIgnore
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = OrderStatus.PAID;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 🔗 Maintain bidirectional consistency
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public Long getJastiperId() {
        return jastiperId;
    }

    public void setJastiperId(Long jastiperId) {
        this.jastiperId = jastiperId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Integer getJastiperRating() {
        return jastiperRating;
    }

    public void setJastiperRating(Integer jastiperRating) {
        this.jastiperRating = jastiperRating;
    }

    public Integer getProductRating() {
        return productRating;
    }

    public void setProductRating(Integer productRating) {
        this.productRating = productRating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}