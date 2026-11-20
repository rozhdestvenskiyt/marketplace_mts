package ru.avito.avito.dto;

import java.util.List;

public record ListingSearchResponse(
        List<ListingResponse> listings,
        long totalElements,
        int page,
        int size,
        int totalPages) {
}
