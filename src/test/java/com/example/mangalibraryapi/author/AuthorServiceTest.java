package com.example.mangalibraryapi.author;

import com.example.mangalibraryapi.manga.MangaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private MangaRepository mangaRepository;

    @InjectMocks
    private AuthorService authorService;


    // findAll

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        void shouldReturnAllAuthors() {
            Author author = new Author();
            author.setName("Eiichiro");
            author.setSurname("Oda");

            when(authorRepository.findAll()).thenReturn(List.of(author));

            List<Author> result = authorService.findAll();

            assertEquals(1, result.size());
            assertEquals("Eiichiro", result.get(0).getName());
            verify(authorRepository, times(1)).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoAuthorsExist() {
            when(authorRepository.findAll()).thenReturn(List.of());

            List<Author> result = authorService.findAll();

            assertTrue(result.isEmpty());
        }
    }


    // createAuthor

    @Nested
    @DisplayName("createAuthor()")
    class CreateAuthor {

        @Test
        void shouldCreateAuthorSuccessfully() {
            AuthorDTO dto = new AuthorDTO("Akira", "Toriyama", "Creator of Dragon Ball");

            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase("Akira", "Toriyama"))
                    .thenReturn(Optional.empty());
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = authorService.createAuthor(dto);

            assertNotNull(result);
            assertEquals("Akira", result.getName());
            assertEquals("Toriyama", result.getSurname());
            assertEquals("Creator of Dragon Ball", result.getBio());

            ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
            verify(authorRepository).save(captor.capture());
            assertEquals("Akira", captor.getValue().getName());
        }

        @Test
        void shouldReturnExistingAuthorWhenNameAndSurnameMatch() {
            Author existing = new Author();
            existing.setId(7L);
            existing.setName("Akira");
            existing.setSurname("Toriyama");

            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase("Akira", "Toriyama"))
                    .thenReturn(Optional.of(existing));

            Author result = authorService.createAuthor(
                    new AuthorDTO("Akira", "Toriyama", "New bio"));

            assertEquals(7L, result.getId());
            assertEquals("Akira", result.getName());
            verify(authorRepository, never()).save(any(Author.class));
        }

        @Test
        void shouldNotDuplicateAuthorWhenNameMatchesOnlyByCase() {
            Author existing = new Author();
            existing.setId(7L);
            existing.setName("Akira");
            existing.setSurname("Toriyama");

            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase("AKIRA", "toriyama"))
                    .thenReturn(Optional.of(existing));

            Author result = authorService.createAuthor(
                    new AuthorDTO("AKIRA", "toriyama", null));

            assertEquals(7L, result.getId());
            verify(authorRepository, never()).save(any(Author.class));
        }
    }


    // updateAuthor

    @Nested
    @DisplayName("updateAuthor()")
    class UpdateAuthor {

        @Test
        void shouldUpdateExistingAuthorFields() {
            Author existing = new Author();
            existing.setId(1L);
            existing.setName("Old");
            existing.setSurname("Name");
            existing.setBio("Old bio");

            when(authorRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

            Author result = authorService.updateAuthor(1L,
                    new AuthorDTO("New", "Name", "New bio"));

            assertEquals("New", result.getName());
            assertEquals("Name", result.getSurname());
            assertEquals("New bio", result.getBio());
            verify(authorRepository, times(1)).save(existing);
        }

        @Test
        void shouldThrowWhenAuthorNotFound() {
            when(authorRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authorService.updateAuthor(99L,
                            new AuthorDTO("X", "Y", "Z")));

            assertEquals("Author not found", ex.getMessage());
            verify(authorRepository, never()).save(any(Author.class));
        }
    }


    // deleteAuthor

    @Nested
    @DisplayName("deleteAuthor()")
    class DeleteAuthor {

        @Test
        void shouldDeleteAuthorWhenNoMangaLinked() {
            when(authorRepository.existsById(1L)).thenReturn(true);
            when(mangaRepository.existsByAuthorId(1L)).thenReturn(false);

            assertDoesNotThrow(() -> authorService.deleteAuthor(1L));

            verify(authorRepository, times(1)).deleteById(1L);
        }

        @Test
        void shouldThrowWhenAuthorNotFound() {
            when(authorRepository.existsById(99L)).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authorService.deleteAuthor(99L));

            assertEquals("Author not found with ID: 99", ex.getMessage());
            verify(authorRepository, never()).deleteById(anyLong());
            verifyNoInteractions(mangaRepository);
        }

        @Test
        void shouldThrowWhenAuthorHasLinkedManga() {
            when(authorRepository.existsById(1L)).thenReturn(true);
            when(mangaRepository.existsByAuthorId(1L)).thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> authorService.deleteAuthor(1L));

            assertEquals("Cannot delete author because they have linked mangas!",
                    ex.getMessage());
            verify(authorRepository, never()).deleteById(anyLong());
        }
    }
}