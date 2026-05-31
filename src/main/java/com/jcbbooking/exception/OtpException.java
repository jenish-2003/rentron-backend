package com.jcbbooking.exception;

import org.springframework.http.HttpStatus;

public class OtpException extends CustomException {
    public OtpException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
