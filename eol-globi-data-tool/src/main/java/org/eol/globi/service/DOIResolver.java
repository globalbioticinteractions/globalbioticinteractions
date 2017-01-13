package org.eol.globi.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DOIResolver {

    Map<String, String> findDOIForReference(Collection<String> references) throws IOException;

    String findDOIForReference(final String reference) throws IOException;

}
