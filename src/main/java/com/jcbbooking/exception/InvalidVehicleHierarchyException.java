package com.jcbbooking.exception;

import org.springframework.http.HttpStatus;

public class InvalidVehicleHierarchyException extends CustomException {
    public InvalidVehicleHierarchyException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
