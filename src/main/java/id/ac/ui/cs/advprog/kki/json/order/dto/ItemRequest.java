package id.ac.ui.cs.advprog.kki.json.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ItemRequest {

    @NotBlank(message = "catalogItemId is required")
    private String catalogItemId;

    @NotNull(message = "qty is required")
    @Min(value = 1, message = "qty must be at least 1")
    private Integer qty;

    @NotNull(message = "priceSnapshot is required")
    @Min(value = 0, message = "priceSnapshot must be >= 0")
    private Double priceSnapshot;

    public String getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(String catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Double getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(Double priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }
}