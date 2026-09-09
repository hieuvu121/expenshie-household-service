package com.be9expensphie.household.exception;

/** Thrown when the addressed household or member does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
