package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenBiodivClient implements SparqlClient {

    private final ResourceService resourceService;

    public OpenBiodivClient(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public LabeledCSVParser query(String sparql) throws IOException {
        try {
            URI url = StudyImporterForPensoft.createSparqlURI(sparql);
            return CSVTSVUtil.createLabeledCSVParser(resourceService.retrieve(url));
        } catch (URISyntaxException | IOException e) {
            throw new IOException("failed to execute query [" + sparql + "]", e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
