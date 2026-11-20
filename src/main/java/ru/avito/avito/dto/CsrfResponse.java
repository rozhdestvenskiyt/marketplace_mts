package ru.avito.avito.dto;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
