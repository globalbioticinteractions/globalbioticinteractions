package org.eol.globi.service;

public class TaxonPropertyLookupServiceException extends Throwable {
    public TaxonPropertyLookupServiceException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public TaxonPropertyLookupServiceException(String msg) {
        super(msg);
    }
}
