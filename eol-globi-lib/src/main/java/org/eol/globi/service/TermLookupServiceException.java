package org.eol.globi.service;

public class TermLookupServiceException extends Throwable {
    public TermLookupServiceException(String msg, Throwable th) {
        super(msg, th);
    }

    public TermLookupServiceException(String msg) {
        super(msg);
    }
}
