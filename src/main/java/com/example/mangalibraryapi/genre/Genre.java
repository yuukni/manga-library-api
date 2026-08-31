package com.example.mangalibraryapi.genre;
import com.example.mangalibraryapi.manga.Manga;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "genres")
    private Set<Manga> mangas = new HashSet<>();

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre genre)) return false;
        return id != null && id.equals(genre.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

