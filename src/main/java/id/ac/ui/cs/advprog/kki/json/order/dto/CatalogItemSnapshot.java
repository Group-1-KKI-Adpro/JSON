package id.ac.ui.cs.advprog.kki.json.order.dto;

public class CatalogItemSnapshot {

    private Integer id;
    private Integer jastiperId;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private String origin;
    private String purchaseDate;
    private String createdAt;
    private String updatedAt;

    public Integer getId() { return id; }
    public Integer getJastiperId() { return jastiperId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getOrigin() { return origin; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setId(Integer id) { this.id = id; }
    public void setJastiperId(Integer jastiperId) { this.jastiperId = jastiperId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Integer price) { this.price = price; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}