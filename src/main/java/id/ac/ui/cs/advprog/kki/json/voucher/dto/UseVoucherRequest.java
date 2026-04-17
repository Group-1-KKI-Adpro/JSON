package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import jakarta.validation.constraints.NotBlank;

public class UseVoucherRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String orderId;

    protected UseVoucherRequest() {}

    public UseVoucherRequest(String code, String orderId) {
        this.code = code;
        this.orderId = orderId;
    }

    public String getCode() {
        return code;
    }

    public String getOrderId() {
        return orderId;
    }
}
