package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import org.springframework.stereotype.Component;

@Component
public class FlatDiscountStrategy implements DiscountStrategy {
    /**
     * Calculates discount as a flat amount.
     * Example: $10 off any order = $10 discount
     *
     * @param originalPrice The original price before discount
     * @param voucher The voucher containing the flat discount value
     * @return The discount amount (capped at the original price)
     */
    @Override
    public double calculateDiscount(double originalPrice, Voucher voucher) {
        // Ensure discount doesn't exceed the original price
        return Math.min(voucher.getDiscountValue(), originalPrice);
    }
}
