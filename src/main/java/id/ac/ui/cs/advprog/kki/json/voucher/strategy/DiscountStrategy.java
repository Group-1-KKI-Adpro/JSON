package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;

public interface DiscountStrategy {
    double calculateDiscount(double originalPrice, Voucher voucher);

    // Each strategy declares which DiscountType it handles so VoucherService
    // can build the dispatch map from a List<DiscountStrategy> without being
    // modified when a new type is added (Open/Closed Principle).
    DiscountType getSupportedType();
}
