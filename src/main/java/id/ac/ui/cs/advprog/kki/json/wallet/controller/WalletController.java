package id.ac.ui.cs.advprog.kki.json.wallet.controller;

import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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

    private Long currentUserId(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        User user = authService.getByEmail(email);
        return user.getId();
    }

    private static TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getUserId(),
                tx.getType(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getCreatedAt(),
                tx.getDescription()
        );
    }

    public record BalanceResponse(Long userId, long balance) {
    }

    public record TopupRequest(@Positive long amount, String description) {
    }

    public record TopupResponse(BalanceResponse balance, TransactionResponse transaction) {
    }

    public record TransactionResponse(
            Long id,
            Long userId,
            TransactionType type,
            long amount,
            TransactionStatus status,
            Instant timestamp,
            String description
    ) {
    }
}

