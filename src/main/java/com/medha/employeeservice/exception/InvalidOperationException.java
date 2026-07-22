package com.medha.employeeservice.exception;

/**
 * Signals a business-rule conflict that is otherwise well-formed (e.g. deleting a
 * department that still has employees assigned, or assigning an employee to a project
 * they already belong to). Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
