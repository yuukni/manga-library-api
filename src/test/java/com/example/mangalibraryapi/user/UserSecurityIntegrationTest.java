package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.manga.Manga;
import com.example.mangalibraryapi.manga.MangaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSecurityIntegrationTest {
    private static final String PASSWORD = "integration-test-password";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired MangaRepository mangas;
    @Autowired UserMangaProgressRepository progresses;
    @Autowired PasswordEncoder encoder;

    private User owner;
    private User other;
    private Manga manga;

    @BeforeEach
    void setUp() {
        owner = createUser("library-owner");
        other = createUser("other-reader");
        manga = new Manga();
        manga.setTitle("Security test manga");
        manga = mangas.saveAndFlush(manga);
    }

    private User createUser(String name) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.com");
        user.setPassword(encoder.encode(PASSWORD));
        return users.saveAndFlush(user);
    }

    private String libraryPath() {
        return "/api/users/" + owner.getId() + "/manga";
    }

    private MockHttpServletRequestBuilder libraryRequest(String method) {
        String path = libraryPath() + (method.equals("DELETE") ? "/" + manga.getId() : "");
        return request(HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mangaTitle\":\"Security test manga\",\"status\":\"READING\",\"currentChapter\":5,\"currentVolume\":1}");
    }

    private void seedProgress() {
        UserMangaProgress progress = new UserMangaProgress();
        progress.setUser(owner);
        progress.setManga(manga);
        progress.setCurrentChapter(2);
        progresses.saveAndFlush(progress);
    }

    @ParameterizedTest
    @CsvSource({"GET,ROLE_USER", "POST,ROLE_USER", "DELETE,ROLE_USER",
            "GET,ROLE_ADMIN", "POST,ROLE_ADMIN", "DELETE,ROLE_ADMIN"})
    void anotherUserCannotReadModifyOrDeleteTheLibrary(String method, String role) throws Exception {
        other.setRole(role);
        users.saveAndFlush(other);
        seedProgress();
        mvc.perform(libraryRequest(method).with(httpBasic(other.getUsername(), PASSWORD)))
                .andExpect(status().isForbidden());
        var remaining = progresses.findByUserAndManga(owner, manga);
        assertThat(remaining).isPresent();
        assertThat(remaining.orElseThrow().getCurrentChapter()).isEqualTo(2);
        assertThat(remaining.orElseThrow().getStatus()).isEqualTo(ReadingStatus.PLAN_TO_READ);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "DELETE"})
    void anonymousRequestsCannotAccessLibraries(String method) throws Exception {
        mvc.perform(libraryRequest(method).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        assertThat(progresses.findByUserAndManga(owner, manga)).isEmpty();
    }

    @Test
    void ownerCanCreateReadUpdateAndDeleteProgress() throws Exception {
        mvc.perform(libraryRequest("POST").with(httpBasic(owner.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentChapter").value(5));
        mvc.perform(get(libraryPath()).with(httpBasic(owner.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manga.id").value(manga.getId()));
        mvc.perform(post(libraryPath()).with(httpBasic(owner.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mangaTitle\":\"Security test manga\",\"status\":\"COMPLETED\",\"currentChapter\":10,\"currentVolume\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentChapter").value(10));
        assertThat(progresses.findByUserId(owner.getId())).hasSize(1);
        mvc.perform(libraryRequest("DELETE").with(httpBasic(owner.getUsername(), PASSWORD)))
                .andExpect(status().isNoContent());
        progresses.flush();
        assertThat(progresses.findByUserAndManga(owner, manga)).isEmpty();
    }

    private ObjectNode registration() {
        return mapper.createObjectNode().put("username", "new-reader")
                .put("email", "new-reader@example.com").put("password", PASSWORD);
    }

    @Test
    void publicRegistrationIgnoresAdminRoleAndPersistsAnEncodedPassword() throws Exception {
        ObjectNode body = registration().put("role", "ROLE_ADMIN");
        mvc.perform(post("/api/registration").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isOk());
        User saved = users.findByUsername("new-reader").orElseThrow();
        assertThat(saved.getRole()).isEqualTo("ROLE_USER");
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, saved.getPassword())).isTrue();
        mvc.perform(get("/api/admin/users").with(httpBasic("new-reader", PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/" + saved.getId() + "/role")
                        .param("role", "ADMIN").with(httpBasic("new-reader", PASSWORD)))
                .andExpect(status().isForbidden());
        assertThat(users.findById(saved.getId()).orElseThrow().getRole()).isEqualTo("ROLE_USER");
        mvc.perform(get("/api/users/" + saved.getId() + "/manga")
                        .with(httpBasic("new-reader", PASSWORD)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"username,empty", "username,blank", "username,missing", "username,null",
            "email,empty", "email,blank", "email,missing", "email,null", "email,invalid",
            "password,empty", "password,blank", "password,missing", "password,null"})
    void invalidRegistrationReturnsBadRequestWithoutPersisting(String field, String variant) throws Exception {
        ObjectNode body = registration();
        switch (variant) {
            case "empty" -> body.put(field, "");
            case "blank" -> body.put(field, "   ");
            case "missing" -> body.remove(field);
            case "null" -> body.putNull(field);
            default -> body.put(field, "invalid-email");
        }
        long before = users.count();
        mvc.perform(post("/api/registration").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))));
        assertThat(users.count()).isEqualTo(before);
    }

    @Test
    void administratorCanChangeRolesButAnonymousRequestsCannot() throws Exception {
        other.setRole("ROLE_ADMIN");
        users.saveAndFlush(other);
        String path = "/api/admin/users/" + owner.getId() + "/role";
        mvc.perform(put(path).param("role", "ADMIN").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        assertThat(users.findById(owner.getId()).orElseThrow().getRole()).isEqualTo("ROLE_USER");
        mvc.perform(put(path).param("role", "ADMIN").with(httpBasic(other.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        assertThat(users.findById(owner.getId()).orElseThrow().getRole()).isEqualTo("ROLE_ADMIN");
    }
}
