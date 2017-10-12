package org.eol.globi.data;

import org.eol.globi.service.DatasetImpl;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertNotNull;

public class StudyImporterForSaproxylicIntegrationTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(StudyImporterForSaproxylic.class);
        importer.setDataset(new DatasetImpl("some/test", URI.create("classpath:/org/eol/globi/data/saproxylic")));
        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Fagus sylvatica"));
        assertNotNull(taxonIndex.findTaxonByName("Epuraea variegata"));
    }

}
