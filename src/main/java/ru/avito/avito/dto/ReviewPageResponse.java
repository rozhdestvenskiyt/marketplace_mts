package ru.avito.avito.dto;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewResponse> reviews,
        long totalElements,
        int page,
        int size,
        int totalPages,
        double averageRating) {
}
