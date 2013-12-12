package org.eol.globi.service;

import java.io.IOException;

public interface DOIResolver {

    public String findDOIForReference(final String reference) throws IOException;

}
