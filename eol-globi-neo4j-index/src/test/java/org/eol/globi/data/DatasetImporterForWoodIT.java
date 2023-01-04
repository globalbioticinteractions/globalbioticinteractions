package org.eol.globi.data;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForWoodIT extends GraphDBNeo4j2TestCase {

    @Test
    public void importFirst500() throws StudyImporterException, IOException {
        DatasetImporterForWood wood = DatasetImporterForWoodTest.createImporter(nodeFactory);

        wood.setFilter(recordNumber -> recordNumber < 500);
        importStudy(wood);

        assertThat(taxonIndex.findTaxonByName("Amphipoda"), is(notNullValue()));
    }

}