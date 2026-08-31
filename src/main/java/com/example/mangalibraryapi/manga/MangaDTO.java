package com.example.mangalibraryapi.manga;

import java.util.List;

public record MangaDTO (String title, String description, Integer year, Integer chapters, Integer volumes, Long authorId, List<Long> genreIds){}
