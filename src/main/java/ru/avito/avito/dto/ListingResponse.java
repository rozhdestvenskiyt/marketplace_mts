package ru.avito.avito.dto;

import java.math.BigDecimal;
import java.util.List;

public record ListingResponse(
        Long id,
        Long sellerId,
        String title,
        String description,
        BigDecimal price,
        List<String> images) {
}
