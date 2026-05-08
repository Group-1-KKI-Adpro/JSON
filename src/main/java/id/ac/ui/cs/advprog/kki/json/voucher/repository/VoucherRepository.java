package id.ac.ui.cs.advprog.kki.json.voucher.repository;

import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {
    List<Voucher> findByActiveTrue();

    // Adds a pessimistic write lock to prevent concurrent modifications
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByIdForUpdate(@Param("code") String code);
}