package id.ac.ui.cs.advprog.kki.json.service;

import id.ac.ui.cs.advprog.kki.json.model.Voucher;
import id.ac.ui.cs.advprog.kki.json.repository.VoucherRepository;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.CreateVoucherRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
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
}