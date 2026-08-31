package com.example.mangalibraryapi.author;

import com.example.mangalibraryapi.manga.MangaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private MangaRepository mangaRepository; // 1. Inject MangaRepository here

    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    public Author createAuthor(AuthorDTO dto) {

        Optional<Author> existingAuthor = authorRepository
                .findByNameIgnoreCaseAndSurnameIgnoreCase(dto.name(), dto.surname());

        if (existingAuthor.isPresent()) {
            return existingAuthor.get();
        }

        Author author = new Author();
        author.setName(dto.name());
        author.setSurname(dto.surname());
        author.setBio(dto.bio());
        return authorRepository.save(author);
    }

    public Author updateAuthor(Long id, AuthorDTO dto) {
        return authorRepository.findById(id)
                .map(author -> {
                    author.setName(dto.name());
                    author.setSurname(dto.surname());
                    author.setBio(dto.bio());
                    return authorRepository.save(author);
                })
                .orElseThrow(() -> new RuntimeException("Author not found"));
    }

    @Transactional
    public void deleteAuthor(Long id) {
        // Check if author exists
        if (!authorRepository.existsById(id)) {
            throw new RuntimeException("Author not found with ID: " + id);
        }

        // PREVENT DELETION if any manga is linked to this author
        if (mangaRepository.existsByAuthorId(id)) {
            throw new IllegalStateException("Cannot delete author because they have linked mangas!");
        }

        // Deletes author if above conditions are met
        authorRepository.deleteById(id);
    }
}