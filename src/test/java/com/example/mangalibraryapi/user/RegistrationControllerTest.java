package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.RegistrationForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;

    @Test
    void shouldRegisterUserAndReturnOk() throws Exception {
        User saved = new User();
        saved.setId(1L);
        saved.setUsername("alice");

        when(userService.registerUser(any(RegistrationForm.class))).thenReturn(saved);

        RegistrationForm form = new RegistrationForm();
        form.setUsername("alice");
        form.setEmail("alice@example.com");
        form.setPassword("pwd");

        mockMvc.perform(post("/api/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully registered")));
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(userService.registerUser(any(RegistrationForm.class)))
                .thenThrow(new RuntimeException("Username already exists!"));

        RegistrationForm form = new RegistrationForm();
        form.setUsername("taken");
        form.setEmail("taken@example.com");
        form.setPassword("pwd");

        mockMvc.perform(post("/api/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username already exists!")));
    }
}