package org.eol.globi.service;

import org.eol.globi.taxon.TermLookupServiceImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class EnvoLookupService extends TermLookupServiceImpl {
    public EnvoLookupService(ResourceService resourceService) {
        super(resourceService);
    }

    @Override
    protected List<URI> getMappingURIList() {
        return new ArrayList<URI>() {{
            try {
                add(getClass().getResource("spire-mapping.tsv").toURI());
            } catch (URISyntaxException e) {
                // ignore
            }
        }};
    }

    @Override
    protected char getDelimiter() {
        return '\t';
    }

    @Override
    protected boolean hasHeader() {
        return false;
    }
}
