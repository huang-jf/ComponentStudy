package com.hjf.router.exceptions;

public final class ParamException extends Exception {

    public ParamException(String message) {
        super(message + " is required param, but didn't contains in the bundle;");
    }

}
