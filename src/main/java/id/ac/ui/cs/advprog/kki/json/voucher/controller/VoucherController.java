package id.ac.ui.cs.advprog.kki.json.voucher.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.CreateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UpdateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.UseVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherRequest;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.ValidateVoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.dto.VoucherResponse;
import id.ac.ui.cs.advprog.kki.json.voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api")
public class VoucherController {

    private final VoucherService voucherService;
    private final AuthService authService;

    public VoucherController(VoucherService voucherService, AuthService authService) {
        this.voucherService = voucherService;
        this.authService = authService;
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

    @PostMapping("/vouchers/validate")
    public ResponseEntity<ValidateVoucherResponse> validateVoucher(@Valid @RequestBody ValidateVoucherRequest request) {
        ValidateVoucherResponse response = voucherService.validateVoucher(request.getCode(), request.getOrderTotal());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vouchers/use")
    public ResponseEntity<UseVoucherResponse> useVoucher(@Valid @RequestBody UseVoucherRequest request,
                                                         Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);

        UseVoucherResponse response = voucherService.useVoucher(
                request.getCode(),
                request.getOrderId(),
                String.valueOf(user.getId())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/vouchers/{code}")
    public ResponseEntity<VoucherResponse> updateVoucher(@PathVariable String code, @Valid @RequestBody UpdateVoucherRequest request) {
        VoucherResponse response = new VoucherResponse(voucherService.updateVoucher(code, request));
        return ResponseEntity.ok(response);
    }
}
