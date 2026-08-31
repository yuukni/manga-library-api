package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.manga.Manga;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserMangaProgressRepository extends JpaRepository<UserMangaProgress, Long> {
    Optional<UserMangaProgress> findByUserAndManga(User user, Manga manga);
    List<UserMangaProgress> findByUserId(Long userId);

    void deleteByUserAndManga(User user, Manga manga);
}