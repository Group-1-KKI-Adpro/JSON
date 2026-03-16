package id.ac.ui.cs.advprog.kki.json.wallet.controller;

import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.VerifyWithdrawRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /*
     * Admin verification step for withdrawals.
     *
     * Flow summary:
     * 1) User creates a withdrawal request via POST /api/wallet/withdraw.
     *    The system stores a WITHDRAW transaction with status=PENDING (no balance change yet).
     * 2) Admin reviews the request out-of-band (manual verification).
     * 3) Admin finalizes it via this endpoint:
     *    - status=SUCCESS: wallet balance is deducted and transaction becomes SUCCESS.
     *    - status=FAILED: wallet balance is NOT changed and transaction becomes FAILED.
     *
     * Expected usage:
     * - Only admins should be able to call this endpoint (see SecurityConfig).
     * - The request body must set status to SUCCESS or FAILED (not PENDING).
     */
    @PostMapping("/withdraw/{transactionId}/verify")
    @Transactional
    public TransactionResponse verifyWithdraw(
            @PathVariable Long transactionId,
            @Valid @RequestBody VerifyWithdrawRequest request
    ) {
        Transaction tx = walletService.verifyWithdraw(transactionId, request.status(), request.failureReason());
        return WalletController.toResponse(tx);
    }
}
