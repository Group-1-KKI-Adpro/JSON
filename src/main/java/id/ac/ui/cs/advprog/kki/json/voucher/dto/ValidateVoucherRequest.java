package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ValidateVoucherRequest {

    @NotBlank
    private String code;

    @NotNull
    @Positive
    private Double orderTotal;

    protected ValidateVoucherRequest() {}

    public ValidateVoucherRequest(String code, Double orderTotal) {
        this.code = code;
        this.orderTotal = orderTotal;
    }

    public String getCode() {
        return code;
    }

    public Double getOrderTotal() {
        return orderTotal;
    }
}
