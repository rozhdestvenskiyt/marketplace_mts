package ru.avito.avito.exception;

import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.avito.avito.dto.ListingRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleApiException(ApiException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception) {
        MethodParameter parameter = exception.getParameter();
        String message = parameter.getParameterType() == ListingRequest.class
                ? "invalid listing data"
                : "invalid request data";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid request data");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleAuthentication(
            AuthenticationException exception) {
        return error(HttpStatus.UNAUTHORIZED, "invalid email or password");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleConflict(
            DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "an account with these details already exists");
    }

    private ResponseEntity<Map<String, Map<String, String>>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", Map.of("message", message)));
    }
}
