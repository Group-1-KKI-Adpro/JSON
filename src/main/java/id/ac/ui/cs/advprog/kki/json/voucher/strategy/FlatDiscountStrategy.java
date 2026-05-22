package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import org.springframework.stereotype.Component;

@Component
public class FlatDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getSupportedType() {
        return DiscountType.FLAT;
    }

    @Override
    public double calculateDiscount(double originalPrice, Voucher voucher) {
        return Math.min(voucher.getDiscountValue(), originalPrice);
    }
}
