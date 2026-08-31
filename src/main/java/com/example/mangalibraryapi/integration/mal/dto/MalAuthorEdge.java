package com.example.mangalibraryapi.integration.mal.dto;

public record MalAuthorEdge(
        MalAuthorNode node,
        String role
) {}