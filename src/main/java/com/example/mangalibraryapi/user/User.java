package com.example.mangalibraryapi.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "role", nullable = false)
    private String role = "ROLE_USER";

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    /**
     * Normalizes any role input to the canonical "ROLE_XXX" format.
     * Accepts: null, "", "USER", "user", "ROLE_USER", "ROLE_user".
     * Always produces: "ROLE_USER", "ROLE_ADMIN", etc.
     */
    public void setRole(String role) {
        if (role == null || role.isBlank()) {
            this.role = "ROLE_USER";
        } else if (!role.startsWith("ROLE_")) {
            this.role = "ROLE_" + role.toUpperCase();
        } else {
            this.role = role.toUpperCase();
        }
    }
}