package com.example.mangalibraryapi.manga;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MangaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MangaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MangaService mangaService;

    @Test
    void shouldReturnAllLocalMangas() throws Exception {
        when(mangaService.findAll()).thenReturn(List.of(new Manga()));

        mockMvc.perform(get("/api/manga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturn404WhenMangaNotFound() throws Exception {
        when(mangaService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/manga/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnMangaByIdWhenExists() throws Exception {
        Manga manga = new Manga();
        manga.setId(1L);
        manga.setTitle("Naruto");
        when(mangaService.findById(1L)).thenReturn(Optional.of(manga));

        mockMvc.perform(get("/api/manga/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Naruto"));
    }

    @Test
    void shouldDeleteAndReturn204() throws Exception {
        mockMvc.perform(delete("/api/manga/1"))
                .andExpect(status().isNoContent());

        verify(mangaService).deleteById(1L);
    }

    @Test
    void shouldImportMangaFromMal() throws Exception {
        when(mangaService.importMangaFromMAL(eq("Naruto"), anyInt()))
                .thenReturn(List.of(new Manga()));

        mockMvc.perform(post("/api/manga/import")
                        .param("query", "Naruto")
                        .param("limit", "5"))
                .andExpect(status().isOk());

        verify(mangaService).importMangaFromMAL("Naruto", 5);
    }
}