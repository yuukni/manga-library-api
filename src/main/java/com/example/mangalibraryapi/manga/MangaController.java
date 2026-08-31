package com.example.mangalibraryapi.manga;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manga")
public class MangaController {

    private final MangaService mangaService;

    public MangaController(MangaService mangaService) {
        this.mangaService = mangaService;
    }

    // 1. External MAL Import Endpoint
    @PostMapping("/import")
    public ResponseEntity<List<Manga>> importManga(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<Manga> importedManga = mangaService.importMangaFromMAL(query, limit);
        return ResponseEntity.ok(importedManga);
    }

    // 2. Local Database Fetching Endpoint (For your regular application flows)
    @GetMapping
    public ResponseEntity<List<Manga>> getAllLocalMangas() {
        return ResponseEntity.ok(mangaService.findAll());
    }

    // 3. Get single local manga by ID
    @GetMapping("/{id}")
    public ResponseEntity<Manga> getMangaById(@PathVariable Long id) {
        return mangaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Delete manga by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMangaById(@PathVariable Long id) {
        mangaService.deleteById(id);
        return ResponseEntity.noContent().build(); // Restituisce HTTP 204 No Content
    }
}