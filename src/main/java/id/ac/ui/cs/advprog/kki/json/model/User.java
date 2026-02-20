package id.ac.ui.cs.advprog.kki.json.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    protected User() {}

    public User(String email) {
        this.email = email;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
}