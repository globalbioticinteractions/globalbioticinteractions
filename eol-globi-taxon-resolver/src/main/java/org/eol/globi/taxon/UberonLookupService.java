package org.eol.globi.taxon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class UberonLookupService extends TermLookupServiceImpl {

    @Override
    protected List<URI> getMappingURIList() {
        try {
            return new ArrayList<URI>() {{
                add(getClass().getResource("body-part-mapping.csv").toURI());
                add(getClass().getResource("life-stage-mapping.csv").toURI());
                add(getClass().getResource("physiological-state-mapping.csv").toURI());
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
