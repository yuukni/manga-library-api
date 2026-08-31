package com.example.mangalibraryapi.user.dto;

import com.example.mangalibraryapi.user.ReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackMangaRequest {
    private String mangaTitle;      // Title of the manga to track
    private ReadingStatus status;   // READING, COMPLETED, etc
    private int currentChapter;     // Current chapter reached
    private int currentVolume;      // Current volume reached
}