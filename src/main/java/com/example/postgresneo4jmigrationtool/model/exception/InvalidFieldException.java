package com.example.postgresneo4jmigrationtool.model.exception;

public class InvalidFieldException extends RuntimeException {

    public InvalidFieldException(String message) {
        super(message);
    }

}
