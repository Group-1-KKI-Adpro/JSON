package id.ac.ui.cs.advprog.kki.json.wallet.repository;

import id.ac.ui.cs.advprog.kki.json.wallet.model.Transaction;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionStatus;
import id.ac.ui.cs.advprog.kki.json.wallet.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Transaction> findTopByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(
            Long userId,
            TransactionType type,
            String referenceId
    );

    Optional<Transaction> findByIdAndType(Long id, TransactionType type);

    List<Transaction> findByTypeAndStatusOrderByCreatedAtAsc(TransactionType type, TransactionStatus status);
}
