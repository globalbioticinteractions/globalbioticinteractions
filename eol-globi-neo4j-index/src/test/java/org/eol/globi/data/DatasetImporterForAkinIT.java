package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DatasetImporterForAkinIT extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(nodeFactory)
                .instantiateImporter(DatasetImporterForAkin.class);
        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Sciaenops ocellatus"));
        assertNotNull(taxonIndex.findTaxonByName("Paralichthys lethostigma"));
        assertNotNull(taxonIndex.findTaxonByName("Adinia xenica"));
        assertNotNull(taxonIndex.findTaxonByName("Citharichthys spilopterus"));
    }

}
