package com.misterfish.exception;

public class UnavailableActionException extends IllegalArgumentException {
    public UnavailableActionException(String s) {
        super(s);
    }
}
