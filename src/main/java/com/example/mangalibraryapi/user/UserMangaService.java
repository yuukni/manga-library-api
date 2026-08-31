package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.manga.Manga;
import com.example.mangalibraryapi.manga.MangaRepository;
import com.example.mangalibraryapi.manga.MangaService;
import com.example.mangalibraryapi.user.dto.TrackMangaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserMangaService {

    private final UserRepository userRepository;
    private final MangaRepository mangaRepository;
    private final MangaService mangaService;
    private final UserMangaProgressRepository progressRepository;

    public UserMangaService(UserRepository userRepository,
                            MangaRepository mangaRepository,
                            MangaService mangaService,
                            UserMangaProgressRepository progressRepository) {
        this.userRepository = userRepository;
        this.mangaRepository = mangaRepository;
        this.mangaService = mangaService;
        this.progressRepository = progressRepository;
    }

    @Transactional
    public UserMangaProgress trackManga(Long userId, TrackMangaRequest request) {
        // Check that the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Search the manga in local db
        Manga manga = mangaRepository.findByTitle(request.getMangaTitle())
                .orElseGet(() -> {
                    // If it doesn't exist, we import it using the MangaService that queries MAL
                    List<Manga> imported = mangaService.importMangaFromMAL(request.getMangaTitle(), 1);
                    if (imported.isEmpty()) {
                        throw new RuntimeException("Manga not found on MyAnimeList: " + request.getMangaTitle());
                    }
                    return imported.get(0); // Return saved manga
                });

        // Create or update the user progress for this manga
        UserMangaProgress progress = progressRepository.findByUserAndManga(user, manga)
                .orElseGet(() -> {
                    UserMangaProgress newProgress = new UserMangaProgress();
                    newProgress.setUser(user);
                    newProgress.setManga(manga);
                    return newProgress;
                });

        // Update progress details
        progress.setStatus(request.getStatus());
        progress.setCurrentChapter(request.getCurrentChapter());
        progress.setCurrentVolume(request.getCurrentVolume());

        return progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public List<UserMangaProgress> getUserLibrary(Long userId) {
        // Check that user exists before returning the list
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return progressRepository.findByUserId(userId);
    }

    @Transactional
    public void removeMangaFromLibrary(Long userId, Long mangaId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Manga manga = mangaRepository.findById(mangaId)
                .orElseThrow(() -> new RuntimeException("Manga not found with id: " + mangaId));

        // Removes only the line in user_manga_progress (the user's library)
        progressRepository.deleteByUserAndManga(user, manga);
    }
}