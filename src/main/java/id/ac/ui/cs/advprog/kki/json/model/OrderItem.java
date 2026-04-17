package id.ac.ui.cs.advprog.kki.json.model;

public class OrderItem {

    private String orderId;
    private String catalogItemId;
    private int qty;
    private Double priceSnapshot;

    public OrderItem() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCatalogItemId() { return catalogItemId; }
    public void setCatalogItemId(String catalogItemId) { this.catalogItemId = catalogItemId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public Double getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(Double priceSnapshot) { this.priceSnapshot = priceSnapshot; }
}