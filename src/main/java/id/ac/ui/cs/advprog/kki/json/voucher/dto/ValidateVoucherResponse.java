package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;

public class ValidateVoucherResponse {

    private final String code;
    private final Double orderTotal;
    private final DiscountType discountType;
    private final Double discountAmount;
    private final Double finalTotal;

    public ValidateVoucherResponse(String code, Double orderTotal, DiscountType discountType, Double discountAmount, Double finalTotal) {
        this.code = code;
        this.orderTotal = orderTotal;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
    }

    public String getCode() {
        return code;
    }

    public Double getOrderTotal() {
        return orderTotal;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public Double getFinalTotal() {
        return finalTotal;
    }
}
