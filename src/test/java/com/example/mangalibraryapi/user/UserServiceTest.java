package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.RegistrationForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegistrationForm form;

    @BeforeEach
    void setUp() {
        form = new RegistrationForm();
        form.setUsername("testUser");
        form.setEmail("test@example.com");
        form.setPassword("clearPassword");
    }

    @Test
    void registerUser_Success() {
        // Given - Setup mock behavior and test data
        when(userRepository.findByUsername(form.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(form.getPassword())).thenReturn("encryptedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername(form.getUsername());
        savedUser.setEmail(form.getEmail());
        savedUser.setPassword("encryptedPassword");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When - Execute the method
        User result = userService.registerUser(form);

        // Then - Verify results and mock interactions
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testUser", result.getUsername());
        assertEquals("encryptedPassword", result.getPassword());

        verify(userRepository, times(1)).findByUsername("testUser");
        verify(passwordEncoder, times(1)).encode("clearPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_ThrowsException_WhenUsernameExists() {
        // Given - Simulate existing user
        User existingUser = new User();
        existingUser.setUsername("testUser");

        when(userRepository.findByUsername(form.getUsername())).thenReturn(Optional.of(existingUser));

        // When & Then - Expect exception and verify no database save occurs
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(form);
        });

        assertEquals("Username already exists!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_Success() {
        // Given - Setup input user and mock behavior
        User inputUser = new User();
        inputUser.setUsername("directUser");
        inputUser.setPassword("rawPassword");

        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("directUser");
        savedUser.setPassword("hashedPassword");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When - Execute the method
        User result = userService.createUser(inputUser);

        // Then - Verify results and mock interactions
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("hashedPassword", result.getPassword());

        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(userRepository, times(1)).save(any(User.class));
    }
}