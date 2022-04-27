package org.eol.globi.taxon;

import org.eol.globi.service.ResourceService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TermLookupServiceWithResource extends TermLookupServiceImpl {

    private final String resourceName;

    public TermLookupServiceWithResource(String resourceName, ResourceService resourceService) {
        super(resourceService);
        this.resourceName = resourceName;
    }

    @Override
    protected List<URI> getMappingURIList() {
        try {
            return new ArrayList<URI>() {{
                add(getClass().getResource(resourceName).toURI());
            }};
        } catch (URISyntaxException e) {
            throw new RuntimeException("failed to read mapping file ", e);
        }
    }

    @Override
    protected char getDelimiter() {
        return ',';
    }

    @Override
    protected boolean hasHeader() {
        return true;
    }
}
