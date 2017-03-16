package org.eol.globi.data;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForWoodIT extends GraphDBTestCase {

    @Test
    public void importFirst500() throws StudyImporterException, IOException {
        StudyImporterForWood wood = StudyImporterForWoodTest.createImporter(nodeFactory);

        wood.setFilter(recordNumber -> recordNumber < 500);
        importStudy(wood);

        assertThat(taxonIndex.findTaxonByName("Amphipoda"), is(notNullValue()));
    }

}