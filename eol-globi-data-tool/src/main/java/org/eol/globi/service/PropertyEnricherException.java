package org.eol.globi.service;

public class PropertyEnricherException extends Throwable {
    public PropertyEnricherException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public PropertyEnricherException(String msg) {
        super(msg);
    }
}
