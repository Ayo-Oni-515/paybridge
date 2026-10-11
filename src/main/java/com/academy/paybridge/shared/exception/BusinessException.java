package com.academy.paybridge.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * A rule was broken (insufficient funds, account not found...). Becomes a clean JSON error.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}