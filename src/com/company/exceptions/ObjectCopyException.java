package com.company.exceptions;

public class ObjectCopyException extends RuntimeException {

    public ObjectCopyException(String message) {
        super(message);
    }

    public ObjectCopyException(String message, Throwable cause) {
        super(message, cause);
    }

}
