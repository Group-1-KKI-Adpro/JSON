package id.ac.ui.cs.advprog.kki.json.voucher.strategy;

import id.ac.ui.cs.advprog.kki.json.model.Voucher;

public interface DiscountStrategy {
    /**
     * Calculates the discount amount based on the original price and voucher details.
     *
     * @param originalPrice The original price before discount
     * @param voucher The voucher containing discount information
     * @return The calculated discount amount
     */
    double calculateDiscount(double originalPrice, Voucher voucher);
}
