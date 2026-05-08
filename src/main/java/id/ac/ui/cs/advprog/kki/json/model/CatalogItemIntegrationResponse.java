package id.ac.ui.cs.advprog.kki.json.model;

public class CatalogItemIntegrationResponse {

    private int price;
    private int stock;
    private int jastiperId;

    public CatalogItemIntegrationResponse(CatalogItem item) {
        this.price = item.getPrice();
        this.stock = item.getStock();
        this.jastiperId = item.getJastiperId();
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public int getJastiperId() {
        return jastiperId;
    }
}