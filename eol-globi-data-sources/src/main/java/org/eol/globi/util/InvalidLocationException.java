package org.eol.globi.util;

public class InvalidLocationException extends Throwable {
    public InvalidLocationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public InvalidLocationException(String msg) {
        super(msg);
    }
}
