package org.eol.globi.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface DOIResolver {

    Map<String, String> resolveDoiFor(Collection<String> references) throws IOException;

    String resolveDoiFor(final String reference) throws IOException;

}
