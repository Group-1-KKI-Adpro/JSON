package id.ac.ui.cs.advprog.kki.json.repository;

import id.ac.ui.cs.advprog.kki.json.model.KycApplication;
import id.ac.ui.cs.advprog.kki.json.model.KycStatus;
import id.ac.ui.cs.advprog.kki.json.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycApplicationRepository extends JpaRepository<KycApplication, Long> {
    Optional<KycApplication> findByUser(User user);
    List<KycApplication> findByStatus(KycStatus status);
}