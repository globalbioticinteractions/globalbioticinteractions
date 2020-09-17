package org.eol.globi.data;

import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertNotNull;

public class DatasetImporterForSaproxylicIntegrationTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForSaproxylic.class);
        importer.setDataset(new DatasetImpl("some/test", URI.create("classpath:/org/eol/globi/data/saproxylic"), inStream -> inStream));
        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Fagus sylvatica"));
        assertNotNull(taxonIndex.findTaxonByName("Epuraea variegata"));
    }

}
