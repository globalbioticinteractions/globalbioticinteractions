package org.eol.globi.data;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
public class DatasetImporterForGrayIT extends GraphDBNeo4jTestCase {

    @Test
    public void importFirst500() throws StudyImporterException, IOException {
        DatasetImporterForGray gray = DatasetImporterForGrayTest.createImporter(nodeFactory, getResourceService());
        gray.setFilter(recordNumber -> recordNumber < 500);
        importStudy(gray);

        assertThat(taxonIndex.findTaxonByName("Staurosira elliptica"), is(notNullValue()));
    }

}