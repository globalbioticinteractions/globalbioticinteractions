package org.eol.globi.data;

import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertNotNull;

public class DatasetImporterForSaproxylicIntegrationTest extends GraphDBNeo4j2TestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForSaproxylic.class);
        importer.setDataset(new DatasetWithResourceMapping("some/test", URI.create("classpath:/org/eol/globi/data/saproxylic"), new ResourceServiceLocalAndRemote(inStream -> inStream)));
        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Fagus sylvatica"));
        assertNotNull(taxonIndex.findTaxonByName("Epuraea variegata"));
    }

}
