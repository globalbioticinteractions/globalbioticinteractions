package org.eol.globi.service;

import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface DOIResolver {

    Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException;

    DOI resolveDoiFor(final String reference) throws IOException;

}
