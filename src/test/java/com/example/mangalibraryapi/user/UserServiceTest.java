package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.RegistrationForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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


    // findAll

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        void shouldReturnAllUsers() {
            User u1 = new User();
            u1.setId(1L);
            u1.setUsername("alice");

            User u2 = new User();
            u2.setId(2L);
            u2.setUsername("bob");

            when(userRepository.findAll()).thenReturn(List.of(u1, u2));

            List<User> result = userService.findAll();

            assertEquals(2, result.size());
            assertEquals("alice", result.get(0).getUsername());
            assertEquals("bob", result.get(1).getUsername());
            verify(userRepository, times(1)).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoUsersExist() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<User> result = userService.findAll();

            assertTrue(result.isEmpty());
            verify(userRepository, times(1)).findAll();
        }
    }


    // createUser (internal/admin use)

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        void shouldEncodePasswordAndSaveUser() {
            User inputUser = new User();
            inputUser.setUsername("directUser");
            inputUser.setPassword("rawPassword");

            when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.createUser(inputUser);

            assertNotNull(result);
            assertEquals("directUser", result.getUsername());
            assertEquals("hashedPassword", result.getPassword());

            verify(passwordEncoder, times(1)).encode("rawPassword");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        void shouldRespectRoleWhenProvided() {
            User inputUser = new User();
            inputUser.setUsername("adminSeed");
            inputUser.setPassword("pwd");
            inputUser.setRole("ROLE_ADMIN");

            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.createUser(inputUser);

            assertEquals("ROLE_ADMIN", result.getRole());
        }

        @Test
        void shouldDefaultToUserRoleWhenNotProvided() {
            User inputUser = new User();
            inputUser.setUsername("plainUser");
            inputUser.setPassword("pwd");
            // Role intentionally not set: entity default should apply.

            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.createUser(inputUser);

            assertEquals("ROLE_USER", result.getRole());
        }
    }


    // deleteUser

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        void shouldDeleteUserWhenExists() {
            when(userRepository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> userService.deleteUser(1L));

            verify(userRepository, times(1)).existsById(1L);
            verify(userRepository, times(1)).deleteById(1L);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.deleteUser(99L));

            assertEquals("User not found with ID: 99", ex.getMessage());
            verify(userRepository, never()).deleteById(anyLong());
        }
    }


    // registerUser (public registration)

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        @Test
        void shouldRegisterUserSuccessfully() {
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("clearPassword")).thenReturn("encryptedPassword");

            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setUsername("testUser");
            savedUser.setEmail("test@example.com");
            savedUser.setPassword("encryptedPassword");
            savedUser.setRole("ROLE_USER");

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.registerUser(form);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("testUser", result.getUsername());
            assertEquals("test@example.com", result.getEmail());
            assertEquals("encryptedPassword", result.getPassword());

            verify(userRepository, times(1)).findByUsername("testUser");
            verify(passwordEncoder, times(1)).encode("clearPassword");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        void shouldAlwaysAssignRoleUser() {
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.registerUser(form);

            assertEquals("ROLE_USER", result.getRole(),
                    "Public registration must always assign ROLE_USER");
        }

        @Test
        void shouldThrowExceptionWhenUsernameExists() {
            User existingUser = new User();
            existingUser.setUsername("testUser");

            when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(existingUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.registerUser(form));

            assertEquals("Username already exists!", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        void shouldNotCallPasswordEncoderWhenUsernameExists() {
            when(userRepository.findByUsername("testUser"))
                    .thenReturn(Optional.of(new User()));

            assertThrows(RuntimeException.class, () -> userService.registerUser(form));

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        void shouldEncodePasswordBeforeSaving() {
            when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("clearPassword")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.registerUser(form);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User persisted = captor.getValue();
            assertEquals("hashed", persisted.getPassword(),
                    "The persisted user must contain the encoded password, not the clear one");
            assertNotEquals("clearPassword", persisted.getPassword());
        }
    }


    // updateRole (admin only)

    @Nested
    @DisplayName("updateRole()")
    class UpdateRole {

        private User existingUser;

        @BeforeEach
        void setUp() {
            existingUser = new User();
            existingUser.setId(1L);
            existingUser.setUsername("bob");
            existingUser.setRole("ROLE_USER");
        }

        @Test
        void shouldPromoteToAdminWithFullPrefix() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateRole(1L, "ROLE_ADMIN");

            assertEquals("ROLE_ADMIN", result.getRole());
            verify(userRepository, times(1)).save(existingUser);
        }

        @Test
        void shouldPromoteToAdminWithLowercaseInput() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateRole(1L, "admin");

            assertEquals("ROLE_ADMIN", result.getRole());
        }

        @Test
        void shouldPromoteToAdminWithoutPrefix() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateRole(1L, "ADMIN");

            assertEquals("ROLE_ADMIN", result.getRole());
        }

        @Test
        void shouldDemoteToUser() {
            existingUser.setRole("ROLE_ADMIN");
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateRole(1L, "USER");

            assertEquals("ROLE_USER", result.getRole());
        }

        @Test
        void shouldThrowExceptionWhenRoleNotInWhitelist() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateRole(1L, "SUPERUSER"));

            assertEquals("Invalid role: SUPERUSER", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldThrowExceptionWhenRoleIsNull() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateRole(1L, null));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldThrowExceptionWhenRoleIsBlank() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateRole(1L, "   "));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.updateRole(99L, "ROLE_ADMIN"));

            assertEquals("User not found with ID: 99", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldNotPersistWhenRoleIsInvalid() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateRole(1L, "GOD_MODE"));

            verify(userRepository, never()).save(any(User.class));
            // The in-memory entity must not be mutated on failure.
            assertEquals("ROLE_USER", existingUser.getRole());
        }
    }
}