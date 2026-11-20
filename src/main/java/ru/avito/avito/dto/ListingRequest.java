package ru.avito.avito.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ListingRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Size(max = 20) List<@NotBlank @Size(max = 2048) String> images) {
}
