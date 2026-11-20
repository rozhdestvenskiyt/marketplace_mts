package ru.avito.avito.dto;

import java.util.List;

public record RegistrationResponse(UserResponse user, List<String> verificationRequired) {
}
