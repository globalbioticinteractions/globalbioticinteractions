package org.globalbioticinteractions.util;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SparqlClientImpl implements SparqlClient {

    private final ResourceService resourceService;
    private final URI endpoint;

    public SparqlClientImpl(ResourceService resourceService, URI endpoint) {
        this.endpoint = endpoint;
        this.resourceService = resourceService;
    }

    @Override
    public LabeledCSVParser query(String queryString) throws IOException {
        try {
            URI url = SparqlUtil.createRequestURI(this.endpoint, queryString);
            return CSVTSVUtil.createLabeledCSVParser(resourceService.retrieve(url));
        } catch (URISyntaxException | IOException e) {
            throw new IOException("failed to execute query [" + queryString + "]", e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
