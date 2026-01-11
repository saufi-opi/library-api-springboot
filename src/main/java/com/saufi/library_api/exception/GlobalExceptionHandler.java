package com.saufi.library_api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatusException(
                        ResponseStatusException ex,
                        HttpServletRequest request) {
                log.error("Response status exception: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(ex.getStatusCode().value())
                                .error(HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase())
                                .message(ex.getReason())
                                .path(request.getRequestURI())
                                .errorCode(HttpStatus.valueOf(ex.getStatusCode().value()).name())
                                .build();
                return new ResponseEntity<>(error, ex.getStatusCode());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(fieldError -> ErrorResponse.ValidationError.builder()
                                                .field(fieldError.getField())
                                                .rejectedValue(maskSensitive(fieldError.getField(),
                                                                fieldError.getRejectedValue()))
                                                .code(fieldError.getCode())
                                                .message(fieldError.getDefaultMessage())
                                                .build())
                                .toList();

                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .message("Invalid input: " + validationErrors.size() + " error(s)")
                                .path(request.getRequestURI())
                                .errorCode("VALIDATION_ERROR")
                                .errors(validationErrors)
                                .build();

                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        private Object maskSensitive(String fieldName, Object value) {
                if (value == null)
                        return null;
                if (fieldName.toLowerCase().contains("password") ||
                                fieldName.toLowerCase().contains("secret") ||
                                fieldName.toLowerCase().contains("token")) {
                        return "***";
                }
                return value;
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex,
                        HttpServletRequest request) {
                log.error("Bad credentials: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("BAD_CREDENTIALS")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
                        HttpServletRequest request) {
                log.error("Authentication failed: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("AUTHENTICATION_FAILED")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
                        HttpServletRequest request) {
                log.error("Access denied: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error("Forbidden")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("ACCESS_DENIED")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex,
                        HttpServletRequest request, HttpServletResponse response) {
                log.warn("Rate limit exceeded: {}", ex.getMessage());

                // Add Retry-After header
                response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                                .error("Too Many Requests")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("RATE_LIMIT_EXCEEDED")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
        }

        @ExceptionHandler(TokenRevokedException.class)
        public ResponseEntity<ErrorResponse> handleTokenRevokedException(TokenRevokedException ex,
                        HttpServletRequest request) {
                log.error("Token revoked: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("TOKEN_REVOKED")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
        public ResponseEntity<ErrorResponse> handleAccountLockedException(
                        org.springframework.security.authentication.LockedException ex,
                        HttpServletRequest request) {
                log.error("Account locked: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.LOCKED.value())
                                .error("Account Locked")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("ACCOUNT_LOCKED")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.LOCKED);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                        HttpServletRequest request) {
                log.error("Internal server error", ex);
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .errorCode("INTERNAL_SERVER_ERROR")
                                .build();
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
