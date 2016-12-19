package org.eol.globi.service;

public class DatasetFinderException extends Exception {
    public DatasetFinderException(Throwable e) {
        super(e);
    }

    public DatasetFinderException(String msg) {
        super(msg);
    }

    public DatasetFinderException(String msg, Throwable e) {
        super(msg, e);

    }
}
