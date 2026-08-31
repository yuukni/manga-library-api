package com.example.mangalibraryapi.genre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/genre")
public class GenreController {

    @Autowired
    private GenreService genreService;

    @GetMapping
    public List<Genre> getall() {
        return genreService.findAll();
    }

    @PostMapping
    public Genre createGenre(@RequestBody GenreDTO genreDTO) {
        return genreService.createGenre(genreDTO);
    }

    @PutMapping("/{id}")
    public Genre updateGenre(@PathVariable Long id, @RequestBody GenreDTO genreDTO) {
        return genreService.updateGenre(id, genreDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
    }
}
