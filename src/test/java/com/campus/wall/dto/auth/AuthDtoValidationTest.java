package com.campus.wall.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void loginDto_usernameTooLong_invalid() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("a".repeat(51));
        dto.setPassword("p");

        var violations = validator.validate(dto);

        assertThat(violations)
            .anyMatch(v -> "username".equals(v.getPropertyPath().toString()));
    }

    @Test
    void refreshTokenDto_illegalFormat_invalid() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("abc$def");

        var violations = validator.validate(dto);

        assertThat(violations)
            .anyMatch(v -> "refreshToken".equals(v.getPropertyPath().toString()));
    }

    @Test
    void refreshTokenDto_tooLong_invalid() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("a".repeat(129));

        var violations = validator.validate(dto);

        assertThat(violations)
            .anyMatch(v -> "refreshToken".equals(v.getPropertyPath().toString()));
    }
}
