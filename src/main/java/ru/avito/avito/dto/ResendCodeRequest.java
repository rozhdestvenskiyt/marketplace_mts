package ru.avito.avito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.avito.avito.entity.VerificationChannel;

public record ResendCodeRequest(
        @NotNull VerificationChannel channel,
        @NotBlank @Size(max = 255) String destination) {
}
