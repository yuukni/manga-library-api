package com.example.mangalibraryapi.exceptions;

import java.time.Instant;

public record ExceptionResponse(Instant data, String message, String description) {
}
