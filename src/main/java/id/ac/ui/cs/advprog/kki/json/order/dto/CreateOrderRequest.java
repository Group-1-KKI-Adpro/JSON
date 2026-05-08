package id.ac.ui.cs.advprog.kki.json.order.dto;

import java.util.List;

public class CreateOrderRequest {

    private String shippingAddress;
    private List<String> items;
    private String voucherCode;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String shippingAddress,
                              List<String> items,
                              String voucherCode) {
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.voucherCode = voucherCode;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}