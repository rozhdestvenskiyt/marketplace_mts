package ru.avito.avito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import ru.avito.avito.dto.UserRequest;

class UserRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRegistrationDetails() {
        UserRequest request = new UserRequest(
                "buyer@example.com",
                "SecurePassword123!",
                "buyer_01",
                "+79991234567",
                true);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidRegistrationDetailsAndMissingConsent() {
        UserRequest request = new UserRequest(
                "not-an-email",
                "short",
                "buyer name",
                "79991234567",
                false);

        assertFalse(validator.validate(request).isEmpty());
    }
}
