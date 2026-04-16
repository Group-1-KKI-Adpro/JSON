package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import jakarta.validation.constraints.NotBlank;

public class UseVoucherRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String orderId;

    @NotBlank
    private String userId;

    protected UseVoucherRequest() {}

    public UseVoucherRequest(String code, String orderId, String userId) {
        this.code = code;
        this.orderId = orderId;
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }
}
