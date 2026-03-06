package id.ac.ui.cs.advprog.kki.json.model;

public class CatalogItemRequest {

    private int jastiperId;
    private String name;
    private String description;
    private int price;
    private int stock;
    private String origin;
    private String purchaseDate;

    public int getJastiperId() {
        return jastiperId;
    }

    public void setJastiperId(int jastiperId) {
        this.jastiperId = jastiperId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}