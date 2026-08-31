package com.example.mangalibraryapi.config;

import com.example.mangalibraryapi.user.User;
import com.example.mangalibraryapi.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public Routes
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers("/api/registration", "/api/registration/**").permitAll()
                        // Delete a manga from the DB (ADMIN ONLY)
                        .requestMatchers(HttpMethod.DELETE, "/api/manga/**").hasRole("ADMIN")
                        // Admin Routes
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Other requests require authentication (login)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler((request, response, authentication) -> {
                            String username = authentication.getName();

                            // Retrieve the user from the DB to get his ID (userId)
                            User user = userRepository.findByUsername(username)
                                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

                            // Redirect to the correct endpoint with its ID!
                            response.sendRedirect("/api/users/" + user.getId() + "/manga");
                        })
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}