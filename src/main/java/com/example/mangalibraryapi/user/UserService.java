package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.RegistrationForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // createUser is used internally (tests, seed data): if role is already set it is respected,
        // otherwise the entity default ("ROLE_USER") applies.
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Public registration: ALWAYS creates a user with the ROLE_USER role.
     * The role is not taken from the form to prevent privilege escalation.
     */
    @Transactional
    public User registerUser(RegistrationForm form) {
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }

        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        // Explicit enforcement: public registration can never elevate privileges.
        user.setRole("ROLE_USER");

        return userRepository.save(user);
    }

    /**
     * Role update: must only be invoked from protected endpoints (AdminController).
     * Validates the role against a whitelist.
     */
    @Transactional
    public User updateRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Normalize before validating (accepts "admin", "ADMIN", "ROLE_ADMIN")
        String normalized = newRole == null ? null
                : (newRole.startsWith("ROLE_") ? newRole : "ROLE_" + newRole).toUpperCase();

        if (normalized == null || !ALLOWED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }

        user.setRole(normalized);
        return userRepository.save(user);
    }
}