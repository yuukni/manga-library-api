package com.example.mangalibraryapi.author;
import com.example.mangalibraryapi.manga.Manga;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String surname;
    private String bio;

    @JsonIgnore
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL) //Author can have multiple Mangas
    private List<Manga> mangas = new ArrayList<>();
}