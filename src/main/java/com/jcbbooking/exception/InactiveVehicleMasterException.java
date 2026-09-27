package com.jcbbooking.exception;

import org.springframework.http.HttpStatus;

public class InactiveVehicleMasterException extends CustomException {
    public InactiveVehicleMasterException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
