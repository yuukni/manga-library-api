package com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.manga.Manga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_manga_progress")
@Getter
@Setter
public class UserMangaProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "manga_id", nullable = false)
    private Manga manga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus status = ReadingStatus.PLAN_TO_READ;

    @Column(name = "current_chapter")
    private int currentChapter = 0;

    @Column(name = "current_volume")
    private int currentVolume = 0;
}