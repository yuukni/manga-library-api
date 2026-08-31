package com.example.mangalibraryapi.manga;

import com.example.mangalibraryapi.genre.Genre;
import com.example.mangalibraryapi.author.Author;
import com.example.mangalibraryapi.author.AuthorRepository;
import com.example.mangalibraryapi.genre.GenreRepository;
import com.example.mangalibraryapi.integration.mal.MyAnimeListClient;
import com.example.mangalibraryapi.integration.mal.dto.MalMangaNode;
import com.example.mangalibraryapi.integration.mal.dto.MalSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MangaService {

    private final MyAnimeListClient malClient;
    private final MangaRepository mangaRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    public MangaService(MyAnimeListClient malClient,
                        MangaRepository mangaRepository,
                        AuthorRepository authorRepository,
                        GenreRepository genreRepository) {
        this.malClient = malClient;
        this.mangaRepository = mangaRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
    }

    public void deleteById(Long id) {
        mangaRepository.deleteById(id);
    }

    /**
     * Searches for manga via MyAnimeList API and imports/updates them in the local database.
     */
    @Transactional
    public List<Manga> importMangaFromMAL(String query, int limit) {
        MalSearchResponse response = malClient.searchManga(query, limit);

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(edge -> edge.node())
                .map(this::saveOrUpdateManga)
                .toList();
    }

    /**
     * Finds all mangas currently saved in the local database.
     */
    @Transactional(readOnly = true)
    public List<Manga> findAll() {
        return mangaRepository.findAll();
    }

    /**
     * Finds a single local manga by its ID.
     */
    @Transactional(readOnly = true)
    public Optional<Manga> findById(Long id) {
        return mangaRepository.findById(id);
    }

    /**
     * Persists a new Manga or updates metadata if it already exists.
     */
    private Manga saveOrUpdateManga(MalMangaNode node) {
        // Find existing manga or instantiate a clean one
        Manga manga = mangaRepository.findByTitle(node.title())
                .orElseGet(Manga::new);

        // Map/Update basic fields
        manga.setTitle(node.title());
        manga.setDescription(node.synopsis());
        manga.setChapters(node.num_chapters() == 0 ? null : node.num_chapters());
        manga.setVolumes(node.num_volumes() == 0 ? null : node.num_volumes());

        // Map and link Genres
        if (node.genres() != null) {
            Set<Genre> genres = new HashSet<>();
            for (var malGenre : node.genres()) {
                Genre genre = (Genre) genreRepository.findByNameIgnoreCase(malGenre.name())
                        .orElseGet(() -> {
                            Genre newGenre = new Genre();
                            newGenre.setName(malGenre.name());
                            return genreRepository.save(newGenre);
                        });
                genres.add(genre);
            }
            manga.setGenres(genres);
        }

        // Map and link the Author
        if (node.authors() != null && !node.authors().isEmpty()) {
            var primaryAuthorNode = node.authors().get(0).node();
            Author author = (Author) authorRepository.findByNameIgnoreCaseAndSurnameIgnoreCase(
                            primaryAuthorNode.first_name(),
                            primaryAuthorNode.last_name())
                    .orElseGet(() -> {
                        Author newAuthor = new Author();
                        newAuthor.setName(primaryAuthorNode.first_name());
                        newAuthor.setSurname(primaryAuthorNode.last_name());
                        return authorRepository.save(newAuthor);
                    });
            manga.setAuthor(author);
        }

        return mangaRepository.save(manga);
    }
}