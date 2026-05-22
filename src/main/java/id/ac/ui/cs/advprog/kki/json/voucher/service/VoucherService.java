package id.ac.ui.cs.advprog.kki.json.voucher.service;

import id.ac.ui.cs.advprog.kki.json.voucher.model.DiscountType;
import id.ac.ui.cs.advprog.kki.json.voucher.strategy.DiscountStrategy;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.CreateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UpdateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import id.ac.ui.cs.advprog.kki.json.voucher.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final Map<DiscountType, DiscountStrategy> discountStrategies;

    public VoucherService(VoucherRepository voucherRepository, List<DiscountStrategy> strategies) {
        this.voucherRepository = voucherRepository;
        this.discountStrategies = strategies.stream()
                .collect(Collectors.toMap(DiscountStrategy::getSupportedType, Function.identity()));
    }

    public Voucher createVoucher(CreateVoucherRequest request) {
        if (voucherRepository.existsById(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher code already exists");
        }

        Voucher voucher = new Voucher(
                request.getCode(),
                request.getQuota(),
                request.getStartAt(),
                request.getEndAt(),
                request.getTerms(),
                request.getDiscountType(),
                request.getDiscountValue(),
                true // Set active to true by default when created
        );

        return voucherRepository.save(voucher);
    }

    public List<Voucher> getActiveVouchers() {
        return voucherRepository.findByActiveTrue();
    }

    public Voucher getVoucherByCode(String code) {
        return voucherRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
    }

    public ValidateVoucherResponse validateVoucher(String code, Double orderTotal) {
        Voucher voucher = getVoucherByCode(code);
        validateVoucherUsability(voucher);

        double discountAmount = calculateDiscount(voucher, orderTotal);
        double finalTotal = Math.max(0.0, orderTotal - discountAmount);
        return new ValidateVoucherResponse(code, orderTotal, voucher.getDiscountType(), discountAmount, finalTotal);
    }

    @Transactional
    public UseVoucherResponse useVoucher(String code, String orderId, String userId) {
        // Fetch the voucher using the locked query to prevent race conditions
        Voucher voucher = voucherRepository.findByIdForUpdate(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
                
        validateVoucherUsability(voucher);

        if (voucher.getQuota() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher quota is exhausted");
        }

        voucher.setQuota(voucher.getQuota() - 1);
        Voucher savedVoucher = voucherRepository.save(voucher);
        return new UseVoucherResponse(code, orderId, userId, savedVoucher.getQuota());
    }

    public Voucher updateVoucher(String code, UpdateVoucherRequest request) {
        Voucher voucher = getVoucherByCode(code);

        if (request.getEndAt() != null) {
            voucher.setEndAt(request.getEndAt());
        }

        if (request.getQuotaToAdd() != null && request.getQuotaToAdd() > 0) {
            if (isExpired(voucher)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add quota to an expired voucher");
            }
            voucher.setQuota(voucher.getQuota() + request.getQuotaToAdd());
        }

        if (request.getActive() != null) {
            voucher.setActive(request.getActive());
        }

        return voucherRepository.save(voucher);
    }

    private void validateVoucherUsability(Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is inactive");
        }
        if (isNotStarted(voucher)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is not active yet");
        }
        if (isExpired(voucher)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is expired");
        }
    }

    private boolean isNotStarted(Voucher voucher) {
        return voucher.getStartAt().isAfter(Instant.now());
    }

    private boolean isExpired(Voucher voucher) {
        return voucher.getEndAt().isBefore(Instant.now());
    }

    private double calculateDiscount(Voucher voucher, Double orderTotal) {
        DiscountStrategy strategy = discountStrategies.get(voucher.getDiscountType());
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown discount type: " + voucher.getDiscountType());
        }
        return strategy.calculateDiscount(orderTotal, voucher);
    }
}
