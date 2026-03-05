package id.ac.ui.cs.advprog.kki.json.voucher.dto;

import id.ac.ui.cs.advprog.kki.json.model.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public class CreateVoucherRequest {

    @NotBlank
    private String code;

    @NotNull
    @Positive
    private Integer quota;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    private String terms;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @Positive
    private Double discountValue;

    protected CreateVoucherRequest() {}

    public CreateVoucherRequest(String code, Integer quota, Instant startAt, Instant endAt, String terms, DiscountType discountType, Double discountValue) {
        this.code = code;
        this.quota = quota;
        this.startAt = startAt;
        this.endAt = endAt;
        this.terms = terms;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public String getCode() { return code; }
    public Integer getQuota() { return quota; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getTerms() { return terms; }
    public DiscountType getDiscountType() { return discountType; }
    public Double getDiscountValue() { return discountValue; }
}