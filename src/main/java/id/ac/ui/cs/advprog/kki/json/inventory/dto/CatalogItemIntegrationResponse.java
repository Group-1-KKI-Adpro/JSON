package id.ac.ui.cs.advprog.kki.json.inventory.dto;

import id.ac.ui.cs.advprog.kki.json.inventory.model.CatalogItem;

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
