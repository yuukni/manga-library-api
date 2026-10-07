package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.manga.Manga;
import com.example.mangalibraryapi.manga.MangaRepository;
import com.example.mangalibraryapi.manga.MangaService;
import com.example.mangalibraryapi.user.dto.TrackMangaRequest;
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
class UserMangaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MangaRepository mangaRepository;

    @Mock
    private MangaService mangaService;

    @Mock
    private UserMangaProgressRepository progressRepository;

    @InjectMocks
    private UserMangaService userMangaService;

    private User user;
    private Manga manga;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");

        manga = new Manga();
        manga.setId(10L);
        manga.setTitle("Naruto");
    }


    // trackManga

    @Nested
    @DisplayName("trackManga()")
    class TrackManga {

        @Test
        void shouldCreateNewProgressWhenMangaExistsLocallyAndNotTracked() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "Naruto", ReadingStatus.READING, 5, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.of(manga));
            when(progressRepository.findByUserAndManga(user, manga)).thenReturn(Optional.empty());
            when(progressRepository.save(any(UserMangaProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserMangaProgress result = userMangaService.trackManga(1L, request);

            assertNotNull(result);
            assertEquals(user, result.getUser());
            assertEquals(manga, result.getManga());
            assertEquals(ReadingStatus.READING, result.getStatus());
            assertEquals(5, result.getCurrentChapter());
            assertEquals(1, result.getCurrentVolume());

            verify(progressRepository, times(1)).save(any(UserMangaProgress.class));
            verify(mangaService, never()).importMangaFromMAL(anyString(), anyInt());
        }

        @Test
        void shouldUpdateExistingProgressWhenMangaAlreadyTracked() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "Naruto", ReadingStatus.COMPLETED, 700, 72);

            UserMangaProgress existing = new UserMangaProgress();
            existing.setId(99L);
            existing.setUser(user);
            existing.setManga(manga);
            existing.setStatus(ReadingStatus.READING);
            existing.setCurrentChapter(10);
            existing.setCurrentVolume(2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findByTitle("Naruto")).thenReturn(Optional.of(manga));
            when(progressRepository.findByUserAndManga(user, manga)).thenReturn(Optional.of(existing));
            when(progressRepository.save(any(UserMangaProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserMangaProgress result = userMangaService.trackManga(1L, request);

            assertEquals(99L, result.getId());
            assertEquals(ReadingStatus.COMPLETED, result.getStatus());
            assertEquals(700, result.getCurrentChapter());
            assertEquals(72, result.getCurrentVolume());

            verify(progressRepository, times(1)).save(existing);
            verify(mangaService, never()).importMangaFromMAL(anyString(), anyInt());
        }

        @Test
        void shouldImportMangaFromMalWhenNotInLocalDatabase() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "One Piece", ReadingStatus.PLAN_TO_READ, 0, 0);

            Manga imported = new Manga();
            imported.setId(20L);
            imported.setTitle("One Piece");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findByTitle("One Piece")).thenReturn(Optional.empty());
            when(mangaService.importMangaFromMAL("One Piece", 1)).thenReturn(List.of(imported));
            when(progressRepository.findByUserAndManga(user, imported)).thenReturn(Optional.empty());
            when(progressRepository.save(any(UserMangaProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserMangaProgress result = userMangaService.trackManga(1L, request);

            assertNotNull(result);
            assertEquals(imported, result.getManga());
            verify(mangaService, times(1)).importMangaFromMAL("One Piece", 1);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "Naruto", ReadingStatus.READING, 0, 0);

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userMangaService.trackManga(99L, request));

            assertEquals("User not found with id: 99", ex.getMessage());
            verifyNoInteractions(mangaRepository, progressRepository);
        }

        @Test
        void shouldThrowWhenMangaCannotBeImportedFromMal() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "Nonexistent Title", ReadingStatus.READING, 0, 0);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findByTitle("Nonexistent Title")).thenReturn(Optional.empty());
            when(mangaService.importMangaFromMAL("Nonexistent Title", 1)).thenReturn(List.of());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userMangaService.trackManga(1L, request));

            assertTrue(ex.getMessage().contains("Manga not found on MyAnimeList"));
            verify(progressRepository, never()).save(any(UserMangaProgress.class));
        }

        @Test
        void shouldUseFirstImportedResultWhenMalReturnsMultipleMatches() {
            TrackMangaRequest request = new TrackMangaRequest(
                    "Berserk", ReadingStatus.READING, 1, 1);

            Manga firstMatch = new Manga();
            firstMatch.setId(30L);
            firstMatch.setTitle("Berserk");

            Manga secondMatch = new Manga();
            secondMatch.setId(31L);
            secondMatch.setTitle("Berserk (Spin-off)");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findByTitle("Berserk")).thenReturn(Optional.empty());
            when(mangaService.importMangaFromMAL("Berserk", 1))
                    .thenReturn(List.of(firstMatch, secondMatch));
            when(progressRepository.findByUserAndManga(user, firstMatch)).thenReturn(Optional.empty());
            when(progressRepository.save(any(UserMangaProgress.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserMangaProgress result = userMangaService.trackManga(1L, request);

            assertEquals(firstMatch, result.getManga());
        }
    }


    // getUserLibrary

    @Nested
    @DisplayName("getUserLibrary()")
    class GetUserLibrary {

        @Test
        void shouldReturnLibraryWhenUserExists() {
            UserMangaProgress p1 = new UserMangaProgress();
            p1.setId(1L);
            UserMangaProgress p2 = new UserMangaProgress();
            p2.setId(2L);

            when(userRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.findByUserId(1L)).thenReturn(List.of(p1, p2));

            List<UserMangaProgress> result = userMangaService.getUserLibrary(1L);

            assertEquals(2, result.size());
            verify(progressRepository, times(1)).findByUserId(1L);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoEntries() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(progressRepository.findByUserId(1L)).thenReturn(List.of());

            List<UserMangaProgress> result = userMangaService.getUserLibrary(1L);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userMangaService.getUserLibrary(99L));

            assertEquals("User not found with id: 99", ex.getMessage());
            verify(progressRepository, never()).findByUserId(anyLong());
        }
    }


    // removeMangaFromLibrary

    @Nested
    @DisplayName("removeMangaFromLibrary()")
    class RemoveMangaFromLibrary {

        @Test
        void shouldDeleteProgressWhenBothUserAndMangaExist() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findById(10L)).thenReturn(Optional.of(manga));

            assertDoesNotThrow(() -> userMangaService.removeMangaFromLibrary(1L, 10L));

            verify(progressRepository, times(1)).deleteByUserAndManga(user, manga);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userMangaService.removeMangaFromLibrary(99L, 10L));

            assertEquals("User not found with id: 99", ex.getMessage());
            verifyNoInteractions(mangaRepository, progressRepository);
        }

        @Test
        void shouldThrowWhenMangaNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(mangaRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userMangaService.removeMangaFromLibrary(1L, 99L));

            assertEquals("Manga not found with id: 99", ex.getMessage());
            verify(progressRepository, never()).deleteByUserAndManga(any(), any());
        }
    }
}