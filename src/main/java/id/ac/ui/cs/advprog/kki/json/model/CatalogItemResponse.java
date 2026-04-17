package id.ac.ui.cs.advprog.kki.json.model;

public class CatalogItemResponse {

    private int id;
    private int jastiperId;
    private String name;
    private String description;
    private int price;
    private int stock;
    private String origin;
    private String purchaseDate;
    private String createdAt;
    private String updatedAt;

    public CatalogItemResponse(CatalogItem item) {
        this.id = item.getId();
        this.jastiperId = item.getJastiperId();
        this.name = item.getName();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.stock = item.getStock();
        this.origin = item.getOrigin();
        this.purchaseDate = item.getPurchaseDate();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }

    public int getId() {
        return id;
    }

    public int getJastiperId() {
        return jastiperId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getOrigin() {
        return origin;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}