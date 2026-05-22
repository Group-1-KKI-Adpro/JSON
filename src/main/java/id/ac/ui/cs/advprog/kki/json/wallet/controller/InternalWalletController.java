package id.ac.ui.cs.advprog.kki.json.wallet.controller;

import id.ac.ui.cs.advprog.kki.json.wallet.dto.DeductRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.RefundRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/wallet")
public class InternalWalletController {

    private final WalletService walletService;

    public InternalWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/deduct")
    @Transactional
    public TransactionResponse deduct(@Valid @RequestBody DeductRequest request) {
        Transaction tx = walletService.deduct(
                request.userId(),
                request.amount(),
                request.referenceId(),
                request.description()
        );
        return WalletController.toResponse(tx);
    }

    @PostMapping("/refund")
    @Transactional
    public TransactionResponse refund(@Valid @RequestBody RefundRequest request) {
        Transaction tx = walletService.refund(
                request.userId(),
                request.amount(),
                request.referenceId(),
                request.description()
        );
        return WalletController.toResponse(tx);
    }
}
