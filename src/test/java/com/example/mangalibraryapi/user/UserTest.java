package com.example.mangalibraryapi.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void setRole_ShouldDefaultToUser_WhenNull() {
        User user = new User();
        user.setRole(null);
        assertEquals("ROLE_USER", user.getRole());
    }

    @Test
    void setRole_ShouldDefaultToUser_WhenBlank() {
        User user = new User();
        user.setRole("   ");
        assertEquals("ROLE_USER", user.getRole());
    }

    @Test
    void setRole_ShouldAddPrefix_WhenMissing() {
        User user = new User();
        user.setRole("admin");
        assertEquals("ROLE_ADMIN", user.getRole());
    }

    @Test
    void setRole_ShouldUppercase_WhenPrefixPresent() {
        User user = new User();
        user.setRole("ROLE_admin");
        assertEquals("ROLE_ADMIN", user.getRole());
    }

    @Test
    void setRole_ShouldKeepValidRole() {
        User user = new User();
        user.setRole("ROLE_USER");
        assertEquals("ROLE_USER", user.getRole());
    }
}