package com.be9expensphie.household.exception;

/** Thrown when the request is valid but conflicts with the household's current state. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
