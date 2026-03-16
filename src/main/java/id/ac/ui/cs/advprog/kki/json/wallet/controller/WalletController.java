package id.ac.ui.cs.advprog.kki.json.wallet.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.BalanceResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.DeductRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.RefundRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TopupRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TopupResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.WithdrawRequest;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final AuthService authService;
    private final WalletService walletService;

    public WalletController(AuthService authService, WalletService walletService) {
        this.authService = authService;
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    @Transactional
    public BalanceResponse getBalance(Authentication authentication) {
        Long userId = currentUserId(authentication);
        Wallet wallet = walletService.getOrCreateWallet(userId);
        return new BalanceResponse(wallet.getUserId(), wallet.getBalance());
    }

    @PostMapping("/topup")
    @Transactional
    public TopupResponse topup(@Valid @RequestBody TopupRequest request,
                               Authentication authentication) {
        Long userId = currentUserId(authentication);
        Transaction tx = walletService.topup(userId, request.amount(), request.description());

        long balanceAfter = tx.getBalanceAfter() == null ? 0L : tx.getBalanceAfter();
        return new TopupResponse(
                new BalanceResponse(userId, balanceAfter),
                toResponse(tx)
        );
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> listTransactions(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return walletService.listTransactions(userId).stream()
                .map(WalletController::toResponse)
                .toList();
    }

    // Internal use (e.g., called by Order / cancellation services). Consider protecting this with a service auth token.
    @PostMapping("/deduct")
    @Transactional
    public TransactionResponse deduct(@Valid @RequestBody DeductRequest request) {
        Transaction tx = walletService.deduct(
                request.userId(),
                request.amount(),
                request.referenceId(),
                request.description()
        );
        return toResponse(tx);
    }

    // Internal use (e.g., called by cancellation services). Consider protecting this with a service auth token.
    @PostMapping("/refund")
    @Transactional
    public TransactionResponse refund(@Valid @RequestBody RefundRequest request) {
        Transaction tx = walletService.refund(
                request.userId(),
                request.amount(),
                request.referenceId(),
                request.description()
        );
        return toResponse(tx);
    }

    @PostMapping("/withdraw")
    @Transactional
    public TransactionResponse withdraw(@Valid @RequestBody WithdrawRequest request,
                                        Authentication authentication) {
        Long userId = currentUserId(authentication);
        // Creates a WITHDRAW transaction with status=PENDING.
        // Balance is NOT deducted here; it is deducted only when an admin verifies SUCCESS.
        Transaction tx = walletService.withdraw(userId, request.amount(), request.description());
        return toResponse(tx);
    }

    private Long currentUserId(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);
        return user.getId();
    }

    static TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getUserId(),
                tx.getType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getCreatedAt(),
                tx.getDescription(),
                tx.getReferenceId(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getFailureReason()
        );
    }
}
