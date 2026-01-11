package com.saufi.library_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenRevokedException extends RuntimeException {

    public TokenRevokedException(String message) {
        super(message);
    }

    public TokenRevokedException() {
        super("Token has been revoked");
    }
}
