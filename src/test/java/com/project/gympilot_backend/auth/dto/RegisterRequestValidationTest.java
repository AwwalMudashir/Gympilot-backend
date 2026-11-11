package com.project.gympilot_backend.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsPasswordsAtEightAndFortyCharacterBoundaries() {
        assertThat(passwordViolations("Aa1!abcd")).isEmpty();
        assertThat(passwordViolations("Aa1!" + "x".repeat(36))).isEmpty();
    }

    @Test
    void rejectsPasswordsOutsideEightAndFortyCharacterBoundaries() {
        assertThat(passwordViolations("Aa1!abc")).isNotEmpty();
        assertThat(passwordViolations("Aa1!" + "x".repeat(37))).isNotEmpty();
    }

    private Set<String> passwordViolations(String password) {
        RegisterRequest request = new RegisterRequest(
                "Ada", "Lovelace", "ada@example.com", password, password);
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("password"))
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }
}
