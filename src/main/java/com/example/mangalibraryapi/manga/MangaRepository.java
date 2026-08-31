package com.example.mangalibraryapi.manga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository public interface MangaRepository extends JpaRepository<Manga,Long> {
    // Spring Data JPA automatically generates the SQL for this query
    boolean existsByAuthorId(Long authorId);
    Optional<Manga> findByTitle(String title);
}