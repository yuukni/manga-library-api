package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.TrackMangaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserMangaController {

    private final UserMangaService userMangaService;

    public UserMangaController(UserMangaService userMangaService) {
        this.userMangaService = userMangaService;
    }

    // Add or updates the reading status of a manga for a specific user
    @PostMapping("/{userId}/manga")
    public ResponseEntity<UserMangaProgress> trackManga(
            @PathVariable Long userId,
            @RequestBody TrackMangaRequest request) {
        return ResponseEntity.ok(userMangaService.trackManga(userId, request));
    }

    // Get the full library of tracked mangas from a specific user
    @GetMapping("/{userId}/manga")
    public ResponseEntity<List<UserMangaProgress>> getUserLibrary(@PathVariable Long userId) {
        return ResponseEntity.ok(userMangaService.getUserLibrary(userId));
    }

    // DELETE /api/users/{userId}/manga/{mangaId}
    @DeleteMapping("/{userId}/manga/{mangaId}")
    public ResponseEntity<Void> removeMangaFromLibrary(
            @PathVariable Long userId,
            @PathVariable Long mangaId) {
        userMangaService.removeMangaFromLibrary(userId, mangaId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}