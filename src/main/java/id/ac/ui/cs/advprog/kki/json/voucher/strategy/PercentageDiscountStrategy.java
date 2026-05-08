package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.model.Voucher;
import org.springframework.stereotype.Component;

@Component
public class PercentageDiscountStrategy implements DiscountStrategy {
    /**
     * Calculates discount as a percentage of the original price.
     * Example: 20% off on a $100 order = $20 discount
     *
     * @param originalPrice The original price before discount
     * @param voucher The voucher containing the percentage discount value
     * @return The discount amount (capped at the original price)
     */
    @Override
    public double calculateDiscount(double originalPrice, Voucher voucher) {
        double discountAmount = originalPrice * (voucher.getDiscountValue() / 100.0);
        // Ensure discount doesn't exceed the original price
        return Math.min(discountAmount, originalPrice);
    }
}
