package org.eol.globi.data;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class DatasetImporterForGrayIT extends GraphDBTestCase {

    @Test
    public void importFirst500() throws StudyImporterException, IOException {
        DatasetImporterForGray gray = DatasetImporterForGrayTest.createImporter(nodeFactory);
        gray.setFilter(recordNumber -> recordNumber < 500);
        importStudy(gray);

        assertThat(taxonIndex.findTaxonByName("Staurosira elliptica"), is(notNullValue()));
    }

}