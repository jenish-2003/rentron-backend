package com.jcbbooking.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends CustomException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
