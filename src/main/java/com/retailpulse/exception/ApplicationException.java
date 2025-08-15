package com.retailpulse.exception;

import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
    private final String errorCode;

    public ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
