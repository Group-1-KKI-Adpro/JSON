package id.ac.ui.cs.advprog.kki.json.voucher.repository;

import id.ac.ui.cs.advprog.kki.json.voucher.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {
    List<Voucher> findByActiveTrue();
}
