package com.be9expensphie.household.exception;

/** Thrown when the caller is authenticated but not allowed to perform this action. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
