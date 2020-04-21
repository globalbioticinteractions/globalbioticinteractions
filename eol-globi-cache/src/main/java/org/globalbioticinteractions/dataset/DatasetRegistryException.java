package org.globalbioticinteractions.dataset;

public class DatasetRegistryException extends Exception {
    public DatasetRegistryException(Throwable e) {
        super(e);
    }

    public DatasetRegistryException(String msg) {
        super(msg);
    }

    public DatasetRegistryException(String msg, Throwable e) {
        super(msg, e);

    }
}
