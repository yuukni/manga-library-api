package com.example.mangalibraryapi.manga;

import com.example.mangalibraryapi.author.Author;
import com.example.mangalibraryapi.author.AuthorRepository;
import com.example.mangalibraryapi.genre.Genre;
import com.example.mangalibraryapi.genre.GenreRepository;
import com.example.mangalibraryapi.integration.mal.MyAnimeListClient;
import com.example.mangalibraryapi.integration.mal.dto.MalAuthorEdge;
import com.example.mangalibraryapi.integration.mal.dto.MalAuthorNode;
import com.example.mangalibraryapi.integration.mal.dto.MalGenre;
import com.example.mangalibraryapi.integration.mal.dto.MalMangaEdge;
import com.example.mangalibraryapi.integration.mal.dto.MalMangaNode;
import com.example.mangalibraryapi.integration.mal.dto.MalSearchResponse;
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
class MangaServiceTest {

    @Mock
    private MyAnimeListClient malClient;

    @Mock
    private MangaRepository mangaRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private MangaService mangaService;

    private MalMangaNode sampleNode;

    @BeforeEach
    void setUp() {
        sampleNode = new MalMangaNode(
                1L,
                "Naruto",
                "A ninja story",
                700,
                72,
                "manga",
                "finished",
                8.5,
                List.of(new MalGenre(1L, "Action"), new MalGenre(2L, "Adventure")),
                List.of(new MalAuthorEdge(
                        new MalAuthorNode(10L, "Masashi", "Kishimoto"),
                        "Story & Art"))
        );
    }


    // importMangaFromMAL

    @Nested
    @DisplayName("importMangaFromMAL()")
    class ImportMangaFromMal {

        @Test
        void shouldPersistNewMangaWithAuthorAndGenres() {
            when(malClient.searchManga("Naruto", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(sampleNode))));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Naruto", 1);

            assertEquals(1, result.size());
            Manga manga = result.get(0);
            assertEquals("Naruto", manga.getTitle());
            assertEquals("A ninja story", manga.getDescription());
            assertEquals(700, manga.getChapters());
            assertEquals(72, manga.getVolumes());
            assertEquals(2, manga.getGenres().size());
            assertNotNull(manga.getAuthor());
            assertEquals("Masashi", manga.getAuthor().getName());
            assertEquals("Kishimoto", manga.getAuthor().getSurname());

            verify(mangaRepository, times(1)).save(any(Manga.class));
            verify(genreRepository, times(2)).save(any(Genre.class));
            verify(authorRepository, times(1)).save(any(Author.class));
        }

        @Test
        void shouldReuseExistingAuthorWhenAlreadyInDatabase() {
            Author existingAuthor = new Author();
            existingAuthor.setId(50L);
            existingAuthor.setName("Masashi");
            existingAuthor.setSurname("Kishimoto");

            when(malClient.searchManga("Naruto", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(sampleNode))));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase("Masashi", "Kishimoto"))
                    .thenReturn(Optional.of(existingAuthor));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Naruto", 1);

            assertEquals(existingAuthor, result.get(0).getAuthor());
            verify(authorRepository, never()).save(any(Author.class));
        }

        @Test
        void shouldReuseExistingGenresWhenAlreadyInDatabase() {
            Genre action = new Genre();
            action.setId(1L);
            action.setName("Action");
            Genre adventure = new Genre();
            adventure.setId(2L);
            adventure.setName("Adventure");

            when(malClient.searchManga("Naruto", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(sampleNode))));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase("Action")).thenReturn(Optional.of(action));
            when(genreRepository.findByNameIgnoreCase("Adventure")).thenReturn(Optional.of(adventure));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.of(new Author()));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Naruto", 1);

            assertEquals(2, result.get(0).getGenres().size());
            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldUpdateExistingMangaWithoutCreatingDuplicate() {
            Manga existing = new Manga();
            existing.setId(99L);
            existing.setTitle("Naruto");
            existing.setChapters(1);

            when(malClient.searchManga("Naruto", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(sampleNode))));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.of(existing));
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.of(new Author()));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Naruto", 1);

            assertEquals(1, result.size());
            assertEquals(99L, result.get(0).getId());
            assertEquals(700, result.get(0).getChapters());
            assertEquals(72, result.get(0).getVolumes());
        }

        @Test
        void shouldSetChaptersAndVolumesToNullWhenZero() {
            MalMangaNode nodeWithZeroCounts = new MalMangaNode(
                    2L, "Ongoing Manga", "Synopsis",
                    0, 0, "manga", "currently_publishing", 7.0,
                    List.of(), List.of());

            when(malClient.searchManga("Ongoing Manga", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(nodeWithZeroCounts))));
            when(mangaRepository.findByTitle("Ongoing Manga")).thenReturn(Optional.empty());
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Ongoing Manga", 1);

            assertNull(result.get(0).getChapters());
            assertNull(result.get(0).getVolumes());
        }

        @Test
        void shouldReturnEmptyListWhenClientReturnsNull() {
            when(malClient.searchManga("Unknown", 5)).thenReturn(null);

            List<Manga> result = mangaService.importMangaFromMAL("Unknown", 5);

            assertTrue(result.isEmpty());
            verifyNoInteractions(mangaRepository, authorRepository, genreRepository);
        }

        @Test
        void shouldReturnEmptyListWhenResponseDataIsNull() {
            when(malClient.searchManga("Unknown", 5))
                    .thenReturn(new MalSearchResponse(null));

            List<Manga> result = mangaService.importMangaFromMAL("Unknown", 5);

            assertTrue(result.isEmpty());
            verifyNoInteractions(mangaRepository);
        }

        @Test
        void shouldReturnEmptyListWhenDataIsEmpty() {
            when(malClient.searchManga("Unknown", 5))
                    .thenReturn(new MalSearchResponse(List.of()));

            List<Manga> result = mangaService.importMangaFromMAL("Unknown", 5);

            assertTrue(result.isEmpty());
            verifyNoInteractions(mangaRepository);
        }

        @Test
        void shouldSkipAuthorLinkingWhenNodeHasNoAuthors() {
            MalMangaNode nodeWithoutAuthors = new MalMangaNode(
                    3L, "Anonymous Manga", "Synopsis",
                    10, 1, "manga", "finished", 6.0,
                    List.of(new MalGenre(1L, "Action")),
                    List.of());

            when(malClient.searchManga("Anonymous Manga", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(nodeWithoutAuthors))));
            when(mangaRepository.findByTitle("Anonymous Manga")).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("Anonymous Manga", 1);

            assertNull(result.get(0).getAuthor());
            verify(authorRepository, never()).save(any(Author.class));
        }

        @Test
        void shouldSkipGenreLinkingWhenNodeHasNoGenres() {
            MalMangaNode nodeWithoutGenres = new MalMangaNode(
                    4L, "No Genres", "Synopsis",
                    5, 1, "manga", "finished", 5.0,
                    List.of(),
                    List.of(new MalAuthorEdge(
                            new MalAuthorNode(10L, "Some", "Author"), "Story")));

            when(malClient.searchManga("No Genres", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(nodeWithoutGenres))));
            when(mangaRepository.findByTitle("No Genres")).thenReturn(Optional.empty());
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("No Genres", 1);

            assertTrue(result.get(0).getGenres().isEmpty());
            verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void shouldImportMultipleMangasInOneCall() {
            MalMangaNode second = new MalMangaNode(
                    5L, "Bleach", "Soul reapers",
                    686, 74, "manga", "finished", 8.0,
                    List.of(), List.of());

            when(malClient.searchManga("shonen", 2))
                    .thenReturn(new MalSearchResponse(List.of(
                            new MalMangaEdge(sampleNode),
                            new MalMangaEdge(second))));
            when(mangaRepository.findByTitle(anyString())).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.of(new Author()));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Manga> result = mangaService.importMangaFromMAL("shonen", 2);

            assertEquals(2, result.size());
            assertEquals("Naruto", result.get(0).getTitle());
            assertEquals("Bleach", result.get(1).getTitle());
            verify(mangaRepository, times(2)).save(any(Manga.class));
        }

        @Test
        void shouldPersistMangaWithCorrectDataFromMalNode() {
            when(malClient.searchManga("Naruto", 1))
                    .thenReturn(new MalSearchResponse(List.of(new MalMangaEdge(sampleNode))));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.empty());
            when(genreRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(anyString(), anyString()))
                    .thenReturn(Optional.of(new Author()));
            when(mangaRepository.save(any(Manga.class))).thenAnswer(inv -> inv.getArgument(0));

            mangaService.importMangaFromMAL("Naruto", 1);

            ArgumentCaptor<Manga> captor = ArgumentCaptor.forClass(Manga.class);
            verify(mangaRepository).save(captor.capture());

            Manga persisted = captor.getValue();
            assertEquals("Naruto", persisted.getTitle());
            assertEquals("A ninja story", persisted.getDescription());
            assertEquals(700, persisted.getChapters());
            assertEquals(72, persisted.getVolumes());
        }
    }


    // findAll

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        void shouldReturnAllMangasFromRepository() {
            Manga m1 = new Manga();
            m1.setId(1L);
            Manga m2 = new Manga();
            m2.setId(2L);

            when(mangaRepository.findAll()).thenReturn(List.of(m1, m2));

            List<Manga> result = mangaService.findAll();

            assertEquals(2, result.size());
            verify(mangaRepository, times(1)).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryIsEmpty() {
            when(mangaRepository.findAll()).thenReturn(List.of());

            List<Manga> result = mangaService.findAll();

            assertTrue(result.isEmpty());
        }
    }


    // findById

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        void shouldReturnMangaWhenExists() {
            Manga manga = new Manga();
            manga.setId(1L);
            manga.setTitle("Naruto");

            when(mangaRepository.findById(1L)).thenReturn(Optional.of(manga));

            Optional<Manga> result = mangaService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals("Naruto", result.get().getTitle());
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            when(mangaRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Manga> result = mangaService.findById(99L);

            assertTrue(result.isEmpty());
        }
    }


    // deleteById

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        void shouldDelegateToRepository() {
            mangaService.deleteById(42L);

            verify(mangaRepository, times(1)).deleteById(42L);
        }

        @Test
        void shouldNotThrowWhenIdDoesNotExist() {
            // Spring Data JPA deleteById is a no-op for missing IDs.
            assertDoesNotThrow(() -> mangaService.deleteById(999L));
            verify(mangaRepository).deleteById(999L);
        }
    }
}