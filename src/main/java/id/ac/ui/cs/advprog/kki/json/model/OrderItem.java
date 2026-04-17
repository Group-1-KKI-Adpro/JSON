package id.ac.ui.cs.advprog.kki.json.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    @Column(nullable = false)
    private Integer catalogItemId;

    @Column(nullable = false)
    private int qty;


    @Column(nullable = false)
    private Long priceSnapshot;

    public OrderItem() {}

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Integer catalogItemId) {
        if (catalogItemId == null) {
            throw new IllegalArgumentException("catalogItemId cannot be null");
        }
        this.catalogItemId = catalogItemId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.qty = qty;
    }

    public Long getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(Long priceSnapshot) {
        if (priceSnapshot == null || priceSnapshot < 0) {
            throw new IllegalArgumentException("Price must be >= 0");
        }
        this.priceSnapshot = priceSnapshot;
    }
}