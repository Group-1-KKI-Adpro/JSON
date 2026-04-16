package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import id.ac.ui.cs.advprog.kki.json.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.model.Voucher;

import java.time.Instant;

public class VoucherResponse {

    private String code;
    private Integer quota;
    private Instant startAt;
    private Instant endAt;
    private String terms;
    private DiscountType discountType;
    private Double discountValue;
    private Boolean active;

    public VoucherResponse(Voucher voucher) {
        this.code = voucher.getCode();
        this.quota = voucher.getQuota();
        this.startAt = voucher.getStartAt();
        this.endAt = voucher.getEndAt();
        this.terms = voucher.getTerms();
        this.discountType = voucher.getDiscountType();
        this.discountValue = voucher.getDiscountValue();
        this.active = voucher.getActive();
    }

    public String getCode() { return code; }
    public Integer getQuota() { return quota; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getTerms() { return terms; }
    public DiscountType getDiscountType() { return discountType; }
    public Double getDiscountValue() { return discountValue; }
    public Boolean getActive() { return active; }
}