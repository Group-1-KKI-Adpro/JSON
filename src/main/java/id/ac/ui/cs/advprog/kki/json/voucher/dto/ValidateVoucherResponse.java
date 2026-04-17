package id.ac.ui.cs.advprog.kki.json.voucher.dto;

public class ValidateVoucherResponse {

    private final String code;
    private final Double orderTotal;
    private final Double discountAmount;
    private final Double finalTotal;

    public ValidateVoucherResponse(String code, Double orderTotal, Double discountAmount, Double finalTotal) {
        this.code = code;
        this.orderTotal = orderTotal;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
    }

    public String getCode() {
        return code;
    }

    public Double getOrderTotal() {
        return orderTotal;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public Double getFinalTotal() {
        return finalTotal;
    }
}
