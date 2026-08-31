package com.example.mangalibraryapi.integration.mal.dto;

import java.util.List;

public record MalMangaNode(
        Long id,
        String title,
        String synopsis, // On MyAnimeList description is called synopsis
        Integer num_chapters,
        Integer num_volumes,
        String media_type,
        String status,
        Double mean,
        List<MalGenre> genres,
        List<MalAuthorEdge> authors
) {}