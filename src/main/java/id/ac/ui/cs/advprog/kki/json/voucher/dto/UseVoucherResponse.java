package id.ac.ui.cs.advprog.kki.json.voucher.dto;

public class UseVoucherResponse {

    private final String code;
    private final String orderId;
    private final String userId;
    private final Integer remainingQuota;

    public UseVoucherResponse(String code, String orderId, String userId, Integer remainingQuota) {
        this.code = code;
        this.orderId = orderId;
        this.userId = userId;
        this.remainingQuota = remainingQuota;
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

    public Integer getRemainingQuota() {
        return remainingQuota;
    }
}
