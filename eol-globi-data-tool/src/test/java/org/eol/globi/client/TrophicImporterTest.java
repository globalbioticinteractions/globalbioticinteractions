package org.eol.globi.client;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.taxon.TaxonLookupService;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class TrophicImporterTest extends GraphDBTestCase {

    @Ignore
    @Test
    public void importStudies() throws IOException, StudyImporterException {
        TrophicImporter trophicImporter = new TrophicImporter();

        GraphDatabaseService graphService = getGraphDb();
        trophicImporter.importStudies(graphService, new TaxonLookupService() {
            @Override
            public String[] lookupTermIds(String taxonName) throws IOException {
                return new String[0];
            }

            @Override
            public void destroy() {

            }
        });

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

}