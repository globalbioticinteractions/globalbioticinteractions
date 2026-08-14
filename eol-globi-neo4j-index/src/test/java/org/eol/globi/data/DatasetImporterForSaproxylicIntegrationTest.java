package org.eol.globi.data;

import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.net.URI;

import static org.junit.Assert.assertNotNull;

public class DatasetImporterForSaproxylicIntegrationTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForSaproxylic.class);
        importer.setDataset(new DatasetWithResourceMapping(
                "some/test",
                URI.create("classpath:/org/eol/globi/data/saproxylic"),
                getResourceService())
        );
        importStudy(importer);

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            assertNotNull(taxonIndex.findTaxonByName("Fagus sylvatica"));
            assertNotNull(taxonIndex.findTaxonByName("Epuraea variegata"));
        }
    }

}
