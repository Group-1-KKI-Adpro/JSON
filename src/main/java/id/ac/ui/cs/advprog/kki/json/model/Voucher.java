package id.ac.ui.cs.advprog.kki.json.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer quota;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(length = 255)
    private String terms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private Boolean active;

    protected Voucher() {}

    public Voucher(String code, Integer quota, Instant startAt, Instant endAt, String terms, DiscountType discountType, Double discountValue, Boolean active) {
        this.code = code;
        this.quota = quota;
        this.startAt = startAt;
        this.endAt = endAt;
        this.terms = terms;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.active = active;
    }

    public String getCode() { return code; }
    public Integer getQuota() { return quota; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getTerms() { return terms; }
    public DiscountType getDiscountType() { return discountType; }
    public Double getDiscountValue() { return discountValue; }
    public Boolean getActive() { return active; }

    public void setCode(String code) { this.code = code; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public void setTerms(String terms) { this.terms = terms; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public void setActive(Boolean active) { this.active = active; }
}