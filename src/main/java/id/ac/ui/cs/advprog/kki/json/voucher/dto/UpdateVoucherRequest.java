package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import jakarta.validation.constraints.Min;

import java.time.Instant;

public class UpdateVoucherRequest {

    private Instant endAt;

    @Min(0)
    private Integer quotaToAdd;

    private Boolean active;

    public UpdateVoucherRequest() {}

    public UpdateVoucherRequest(Instant endAt, Integer quotaToAdd, Boolean active) {
        this.endAt = endAt;
        this.quotaToAdd = quotaToAdd;
        this.active = active;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Integer getQuotaToAdd() {
        return quotaToAdd;
    }

    public Boolean getActive() {
        return active;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public void setQuotaToAdd(Integer quotaToAdd) {
        this.quotaToAdd = quotaToAdd;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
