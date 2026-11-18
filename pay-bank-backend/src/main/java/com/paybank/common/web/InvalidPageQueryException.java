package com.paybank.common.web;

/** Pagination parameters out of range. */
public class InvalidPageQueryException extends RuntimeException {

    public InvalidPageQueryException(String message) {
        super(message);
    }
}
