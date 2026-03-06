package id.ac.ui.cs.advprog.kki.json.wallet.repository;

import id.ac.ui.cs.advprog.kki.json.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
