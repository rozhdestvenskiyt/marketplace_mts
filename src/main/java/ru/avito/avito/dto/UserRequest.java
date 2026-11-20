package ru.avito.avito.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "^[\\p{L}\\p{N}_.-]+$")
        String username,
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
        @NotNull @AssertTrue Boolean termsAccepted) {
}
