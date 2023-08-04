package org.eol.globi.tool;

import org.eol.globi.data.NodeFactoryException;

public class UnlikelyTaxonNameException extends NodeFactoryException {
    public UnlikelyTaxonNameException(String msg) {
        super(msg);
    }
}
