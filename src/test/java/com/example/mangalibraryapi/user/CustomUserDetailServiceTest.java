package com.example.mangalibraryapi.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;


    // loadUserByUsername

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        void shouldReturnUserDetailsWithAdminAuthority() {
            User user = new User();
            user.setUsername("admin");
            user.setPassword("hashed");
            user.setRole("ROLE_ADMIN");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("admin");

            assertEquals("admin", details.getUsername());
            assertEquals("hashed", details.getPassword());
            assertTrue(details.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        void shouldReturnUserDetailsWithUserAuthority() {
            User user = new User();
            user.setUsername("alice");
            user.setPassword("hashed");
            user.setRole("ROLE_USER");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("alice");

            assertTrue(details.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        }

        @Test
        void shouldDefaultToUserAuthorityWhenRoleIsNull() {
            User user = new User();
            user.setUsername("plain");
            user.setPassword("hashed");
            user.setRole(null);
            when(userRepository.findByUsername("plain")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("plain");

            assertTrue(details.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                    () -> service.loadUserByUsername("ghost"));

            assertEquals("User not found with username: ghost", ex.getMessage());
        }
    }
}