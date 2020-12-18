package org.eol.globi.service;

public class TermLookupServiceConfigurationException extends TermLookupServiceException {
    public TermLookupServiceConfigurationException(String msg, Throwable th) {
        super(msg, th);
    }

    public TermLookupServiceConfigurationException(String msg) {
        super(msg);
    }
}
