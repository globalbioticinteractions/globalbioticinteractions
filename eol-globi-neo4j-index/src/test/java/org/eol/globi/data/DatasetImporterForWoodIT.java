package org.eol.globi.data;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForWoodIT extends GraphDBTestCase {

    @Test
    public void importFirst500() throws StudyImporterException, IOException {
        DatasetImporterForWood wood = DatasetImporterForWoodTest.createImporter(nodeFactory, getResourceService());

        wood.setFilter(recordNumber -> recordNumber < 500);
        importStudy(wood);

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            assertThat(taxonIndex.findTaxonByName("Amphipoda"), is(notNullValue()));
        }
    }

}