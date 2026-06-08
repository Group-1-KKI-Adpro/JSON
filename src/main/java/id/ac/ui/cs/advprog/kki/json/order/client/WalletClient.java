package id.ac.ui.cs.advprog.kki.json.order.client;

import id.ac.ui.cs.advprog.kki.json.auth.security.JwtService;
import id.ac.ui.cs.advprog.kki.json.auth.service.AuthService;
import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.service.WalletService;
import org.springframework.stereotype.Component;

@Component
public class WalletClient {

    private final WalletService walletService;
    private final JwtService jwtService;
    private final AuthService authService;

    public WalletClient(WalletService walletService,
                        JwtService jwtService,
                        AuthService authService) {
        this.walletService = walletService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    public long getBalance(String token) {
        User user = userFromToken(token);
        return walletService.getOrCreateWallet(user.getId()).getBalance();
    }

    public TransactionResponse deduct(Long userId,
                                      Long amount,
                                      String referenceId,
                                      String description) {
        return toResponse(
                walletService.deduct(userId, amount, referenceId, description)
        );
    }

    public TransactionResponse refund(Long userId,
                                      Long amount,
                                      String referenceId,
                                      String description) {
        return toResponse(
                walletService.refund(userId, amount, referenceId, description)
        );
    }

    private User userFromToken(String token) {
        String email = jwtService.extractEmail(token);
        return authService.getByEmail(email);
    }

    private TransactionResponse toResponse(Transaction tx) {
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
