package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.TrackMangaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserMangaController {

    private final UserMangaService userMangaService;
    private final UserRepository userRepository;

    public UserMangaController(UserMangaService userMangaService, UserRepository userRepository) {
        this.userMangaService = userMangaService;
        this.userRepository = userRepository;
    }

    // Add or updates the reading status of a manga for a specific user
    @PostMapping("/{userId}/manga")
    public ResponseEntity<UserMangaProgress> trackManga(
            @PathVariable Long userId,
            @RequestBody TrackMangaRequest request, Authentication authentication) {
        requireOwner(userId, authentication);
        return ResponseEntity.ok(userMangaService.trackManga(userId, request));
    }

    // Get the full library of tracked mangas from a specific user
    @GetMapping("/{userId}/manga")
    public ResponseEntity<List<UserMangaProgress>> getUserLibrary(@PathVariable Long userId, Authentication authentication) {
        requireOwner(userId, authentication);
        return ResponseEntity.ok(userMangaService.getUserLibrary(userId));
    }

    // DELETE /api/users/{userId}/manga/{mangaId}
    @DeleteMapping("/{userId}/manga/{mangaId}")
    public ResponseEntity<Void> removeMangaFromLibrary(
            @PathVariable Long userId,
            @PathVariable Long mangaId, Authentication authentication) {
        requireOwner(userId, authentication);
        userMangaService.removeMangaFromLibrary(userId, mangaId);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    private void requireOwner(Long userId, Authentication authentication) {
        boolean isOwner = authentication != null && authentication.isAuthenticated()
                && userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(userId))
                .orElse(false);
        if (!isOwner) {
            throw new AccessDeniedException("You can only access your own library");
        }
    }
}
