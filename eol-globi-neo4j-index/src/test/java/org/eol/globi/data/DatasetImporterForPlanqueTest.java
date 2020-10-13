package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DatasetImporterForPlanqueTest extends GraphDBTestCase {

    @Test
    public void taxonNameParser() {
        assertThat(DatasetImporterForPlanque.normalizeName("SAGITTA_ELEGANS"), is("Sagitta elegans"));
        assertThat(DatasetImporterForPlanque.normalizeName("OSTRACODA_INDET"), is("Ostracoda"));
    }

}
