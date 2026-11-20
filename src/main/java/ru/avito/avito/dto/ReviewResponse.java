package ru.avito.avito.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long listingId,
        Long authorId,
        String authorName,
        int rating,
        String comment,
        Instant createdAt) {
}
