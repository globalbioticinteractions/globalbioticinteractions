package org.trophic.graph.client;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.StudyImporterException;
import org.trophic.graph.data.taxon.TaxonLookupService;

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
            public long[] lookupTerms(String taxonName) throws IOException {
                return new long[0];
            }

            @Override
            public void destroy() {

            }
        });

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

}