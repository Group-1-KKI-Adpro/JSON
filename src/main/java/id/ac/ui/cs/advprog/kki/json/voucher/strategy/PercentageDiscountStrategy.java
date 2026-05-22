package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import org.springframework.stereotype.Component;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getSupportedType() {
        return DiscountType.PERCENTAGE;
    }

    @Override
    public double calculateDiscount(double originalPrice, Voucher voucher) {
        double discountAmount = originalPrice * (voucher.getDiscountValue() / 100.0);
        return Math.min(discountAmount, originalPrice);
    }
}
