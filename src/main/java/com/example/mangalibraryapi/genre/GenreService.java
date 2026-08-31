package com.example.mangalibraryapi.genre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreService {
    @Autowired
    private GenreRepository genreRepository;

    public List<Genre> findAll() { return genreRepository.findAll(); }

    public Genre createGenre(GenreDTO dto) {
        Optional<Genre> existingGenre = genreRepository.findByNameIgnoreCase(dto.name());
        if (existingGenre.isPresent()) {
            return existingGenre.get();
        }
        Genre genre = new Genre();
        genre.setName(dto.name());
        return genreRepository.save(genre);
    }

    public Genre updateGenre(Long id, GenreDTO dto) {
        return genreRepository.findById(id)
                .map(genre -> {
                    genre.setName(dto.name());
                    return genreRepository.save(genre);
                })
                .orElseThrow(() -> new RuntimeException("Genre not found"));
    }

    public void deleteGenre(Long id) {
        genreRepository.deleteById(id);
    }
}
