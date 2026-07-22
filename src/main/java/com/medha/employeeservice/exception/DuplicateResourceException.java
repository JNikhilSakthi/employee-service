package com.medha.employeeservice.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super("%s already exists with %s='%s'".formatted(resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
