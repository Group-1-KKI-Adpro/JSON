package id.ac.ui.cs.advprog.kki.json.controller;

import id.ac.ui.cs.advprog.kki.json.service.VoucherService;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.CreateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.VoucherResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/admin/vouchers")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        VoucherResponse response = new VoucherResponse(voucherService.createVoucher(request));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> getActiveVouchers() {
        List<VoucherResponse> responses = voucherService.getActiveVouchers()
                .stream()
                .map(VoucherResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/vouchers/{code}")
    public ResponseEntity<VoucherResponse> getVoucherByCode(@PathVariable String code) {
        VoucherResponse response = new VoucherResponse(voucherService.getVoucherByCode(code));
        return ResponseEntity.ok(response);
    }
}