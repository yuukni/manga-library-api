package com.example.mangalibraryapi.genre;

import org.junit.jupiter.api.BeforeEach;
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
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;


    // findAll

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        void shouldReturnAllGenres() {
            Genre g1 = new Genre();
            g1.setId(1L);
            g1.setName("Action");
            Genre g2 = new Genre();
            g2.setId(2L);
            g2.setName("Adventure");

            when(genreRepository.findAll()).thenReturn(List.of(g1, g2));

            List<Genre> result = genreService.findAll();

            assertEquals(2, result.size());
            assertEquals("Action", result.get(0).getName());
            verify(genreRepository, times(1)).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoGenresExist() {
            when(genreRepository.findAll()).thenReturn(List.of());

            List<Genre> result = genreService.findAll();

            assertTrue(result.isEmpty());
        }
    }


    // createGenre

    @Nested
    @DisplayName("createGenre()")
    class CreateGenre {

        @Test
        void shouldPersistNewGenreWhenNameDoesNotExist() {
            when(genreRepository.findByNameIgnoreCase("Action")).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

            Genre result = genreService.createGenre(new GenreDTO("Action"));

            assertNotNull(result);
            assertEquals("Action", result.getName());

            ArgumentCaptor<Genre> captor = ArgumentCaptor.forClass(Genre.class);
            verify(genreRepository).save(captor.capture());
            assertEquals("Action", captor.getValue().getName());
        }

        @Test
        void shouldReturnExistingGenreWhenNameMatchesExactly() {
            Genre existing = new Genre();
            existing.setId(1L);
            existing.setName("Action");

            when(genreRepository.findByNameIgnoreCase("Action")).thenReturn(Optional.of(existing));

            Genre result = genreService.createGenre(new GenreDTO("Action"));

            assertEquals(1L, result.getId());
            assertEquals("Action", result.getName());
            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldReturnExistingGenreWhenNameDiffersOnlyByCase() {
            Genre existing = new Genre();
            existing.setId(5L);
            existing.setName("Action");

            when(genreRepository.findByNameIgnoreCase("action")).thenReturn(Optional.of(existing));

            Genre result = genreService.createGenre(new GenreDTO("action"));

            assertEquals(5L, result.getId());
            assertEquals("Action", result.getName());
            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldNotDuplicateGenreRegardlessOfCase() {
            Genre existing = new Genre();
            existing.setId(1L);
            existing.setName("Sci-Fi");

            when(genreRepository.findByNameIgnoreCase(anyString()))
                    .thenReturn(Optional.of(existing));

            genreService.createGenre(new GenreDTO("SCI-FI"));

            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldPersistGenreWithEmptyNameIfRepositoryAllowsIt() {
            // Documents current behavior: no validation on name.
            when(genreRepository.findByNameIgnoreCase("")).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

            Genre result = genreService.createGenre(new GenreDTO(""));

            assertNotNull(result);
            assertEquals("", result.getName());
            verify(genreRepository).save(any(Genre.class));
        }
    }


    // updateGenre

    @Nested
    @DisplayName("updateGenre()")
    class UpdateGenre {

        @Test
        void shouldUpdateNameWhenGenreExists() {
            Genre existing = new Genre();
            existing.setId(1L);
            existing.setName("Old Name");

            when(genreRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

            Genre result = genreService.updateGenre(1L, new GenreDTO("New Name"));

            assertEquals(1L, result.getId());
            assertEquals("New Name", result.getName());
            verify(genreRepository, times(1)).save(existing);
        }

        @Test
        void shouldThrowWhenGenreNotFound() {
            when(genreRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> genreService.updateGenre(99L, new GenreDTO("X")));

            assertEquals("Genre not found", ex.getMessage());
            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldNotPersistWhenGenreNotFound() {
            when(genreRepository.findById(42L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> genreService.updateGenre(42L, new GenreDTO("Anything")));

            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldOverwriteExistingNameWithNewOne() {
            Genre existing = new Genre();
            existing.setId(1L);
            existing.setName("Adventure");

            when(genreRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

            Genre result = genreService.updateGenre(1L, new GenreDTO("Fantasy"));

            assertEquals("Fantasy", result.getName());
        }
    }


    // deleteGenre

    @Nested
    @DisplayName("deleteGenre()")
    class DeleteGenre {

        @Test
        void shouldDelegateToRepository() {
            genreService.deleteGenre(1L);

            verify(genreRepository, times(1)).deleteById(1L);
        }

        @Test
        void shouldNotThrowWhenIdDoesNotExist() {
            assertDoesNotThrow(() -> genreService.deleteGenre(999L));
            verify(genreRepository).deleteById(999L);
        }

        @Test
        void shouldCallDeleteByIdExactlyOnce() {
            genreService.deleteGenre(42L);

            verify(genreRepository, times(1)).deleteById(42L);
            verifyNoMoreInteractions(genreRepository);
        }
    }
}