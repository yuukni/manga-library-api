package com.example.mangalibraryapi.user.dto;

import lombok.Data;

@Data
public class RegistrationForm {
    private String username;
    private String email;
    private String password;
}