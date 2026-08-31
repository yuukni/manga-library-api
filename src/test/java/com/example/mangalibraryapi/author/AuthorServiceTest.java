package com.example.mangalibraryapi.author;

import com.example.mangalibraryapi.manga.MangaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private MangaRepository mangaRepository;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void shouldReturnAllAuthors() {
        Author author = new Author();
        author.setName("Eiichiro");
        author.setSurname("Oda");

        when(authorRepository.findAll()).thenReturn(List.of(author));

        List<Author> result = authorService.findAll();

        assertEquals(1, result.size());
        assertEquals("Eiichiro", result.getFirst().getName());
        verify(authorRepository, times(1)).findAll();
    }

    @Test
    void shouldCreateAuthorSuccessfully() {
        AuthorDTO dto = new AuthorDTO("Akira", "Toriyama", "Creator of Dragon Ball");
        Author savedAuthor = new Author();
        savedAuthor.setId(1L);
        savedAuthor.setName(dto.name());

        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

        Author result = authorService.createAuthor(dto);

        assertNotNull(result);
        assertEquals("Akira", result.getName());
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void shouldDeleteAuthorWhenNoMangaLinked() {
        Long authorId = 1L;

        when(authorRepository.existsById(authorId)).thenReturn(true);
        when(mangaRepository.existsByAuthorId(authorId)).thenReturn(false);

        assertDoesNotThrow(() -> authorService.deleteAuthor(authorId));

        verify(authorRepository, times(1)).deleteById(authorId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingAuthorWithLinkedManga() {
        Long authorId = 1L;

        when(authorRepository.existsById(authorId)).thenReturn(true);
        when(mangaRepository.existsByAuthorId(authorId)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authorService.deleteAuthor(authorId)
        );

        assertEquals("Cannot delete author because they have linked mangas!", exception.getMessage());
        verify(authorRepository, never()).deleteById(authorId);
    }
}