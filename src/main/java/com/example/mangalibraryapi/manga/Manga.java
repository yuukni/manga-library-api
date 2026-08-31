package com.example.mangalibraryapi.manga;
import com.example.mangalibraryapi.author.Author;
import com.example.mangalibraryapi.genre.Genre;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Manga {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Integer year;
    private Integer chapters;
    private Integer volumes;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToMany
    @JoinTable(
            name = "manga_genre",
            joinColumns = @JoinColumn(name = "manga_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Manga manga)) return false;
        return id != null && id.equals(manga.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}