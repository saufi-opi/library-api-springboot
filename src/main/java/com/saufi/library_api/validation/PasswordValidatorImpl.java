package com.saufi.library_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidatorImpl implements ConstraintValidator<ValidPassword, String> {

    @Value("${application.security.password.min-length}")
    private int minLength;

    @Value("${application.security.password.require-uppercase}")
    private boolean requireUppercase;

    @Value("${application.security.password.require-lowercase}")
    private boolean requireLowercase;

    @Value("${application.security.password.require-digit}")
    private boolean requireDigit;

    @Value("${application.security.password.require-special-char}")
    private boolean requireSpecialChar;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        // Check minimum length
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                    "Password must be at least " + minLength + " characters long").addConstraintViolation();
            isValid = false;
        }

        // Check for uppercase letter
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter").addConstraintViolation();
            isValid = false;
        }

        // Check for lowercase letter
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter").addConstraintViolation();
            isValid = false;
        }

        // Check for digit
        if (requireDigit && !password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one digit").addConstraintViolation();
            isValid = false;
        }

        // Check for special character
        if (requireSpecialChar && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
