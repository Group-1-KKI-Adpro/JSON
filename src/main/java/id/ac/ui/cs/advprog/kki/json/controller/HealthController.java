package id.ac.ui.cs.advprog.kki.json.controller;

import id.ac.ui.cs.advprog.kki.json.model.User;
import id.ac.ui.cs.advprog.kki.json.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final UserRepository userRepository;

    public HealthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/health")
    public String health() {
        userRepository.save(new User("demo@json.com"));
        return "JSON backend alive. userCount=" + userRepository.count();
    }
}