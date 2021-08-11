package org.eol.globi.data;

import java.util.IllegalFormatException;

public class IllegalNODCTaxonCodeException extends IllegalArgumentException {

    private String msg;

    private IllegalNODCTaxonCodeException() {
        super();
    }

    public IllegalNODCTaxonCodeException(String msg) {
        if (msg == null) {
            throw new NullPointerException();
        }
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }

}
