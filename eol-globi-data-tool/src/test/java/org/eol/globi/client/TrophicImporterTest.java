package org.eol.globi.client;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class TrophicImporterTest extends GraphDBTestCase {

    @Ignore
    @Test
    public void importStudies() throws IOException, StudyImporterException {
        TrophicImporter trophicImporter = new TrophicImporter();

        GraphDatabaseService graphService = getGraphDb();
        trophicImporter.importStudies(graphService, new TaxonPropertyEnricher() {
            @Override
            public boolean enrich(Taxon taxon) throws IOException {
                return false;
            }
        });

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

}