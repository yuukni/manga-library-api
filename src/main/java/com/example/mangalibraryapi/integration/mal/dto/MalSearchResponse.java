package com.example.mangalibraryapi.integration.mal.dto;

import java.util.List;

public record MalSearchResponse(
        List<MalMangaEdge> data
) {}