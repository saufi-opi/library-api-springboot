package com.saufi.library_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomHttpException extends RuntimeException {

    private final HttpStatus status;

    public CustomHttpException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static CustomHttpException notFound(String message) {
        return new CustomHttpException(message, HttpStatus.NOT_FOUND);
    }

    public static CustomHttpException badRequest(String message) {
        return new CustomHttpException(message, HttpStatus.BAD_REQUEST);
    }

    public static CustomHttpException forbidden(String message) {
        return new CustomHttpException(message, HttpStatus.FORBIDDEN);
    }

    public static CustomHttpException unauthorized(String message) {
        return new CustomHttpException(message, HttpStatus.UNAUTHORIZED);
    }

    public static CustomHttpException conflict(String message) {
        return new CustomHttpException(message, HttpStatus.CONFLICT);
    }
}
