package id.ac.ui.cs.advprog.kki.json.repository;

import id.ac.ui.cs.advprog.kki.json.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}