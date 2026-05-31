package com.jcbbooking.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends CustomException {
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
