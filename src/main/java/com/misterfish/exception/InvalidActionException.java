package com.misterfish.exception;

public class InvalidActionException extends IllegalArgumentException {
    public InvalidActionException(String s) {
        super(s);
    }
}
