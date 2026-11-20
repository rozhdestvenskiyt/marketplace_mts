package ru.avito.avito.dto;

public record VerificationResponse(
        boolean emailVerified,
        boolean phoneVerified,
        boolean accountVerified) {
}
