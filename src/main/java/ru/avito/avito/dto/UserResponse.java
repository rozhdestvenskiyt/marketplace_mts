package ru.avito.avito.dto;

public record UserResponse(
        Long id,
        String email,
        String username,
        String phone,
        boolean emailVerified,
        boolean phoneVerified) {
}
