package id.ac.ui.cs.advprog.kki.json.repository;

import id.ac.ui.cs.advprog.kki.json.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}