package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForPlanqueTest {

    @Test
    public void taxonNameParser() {
        assertThat(StudyImporterForPlanque.normalizeName("SAGITTA_ELEGANS"), is("Sagitta elegans"));
        assertThat(StudyImporterForPlanque.normalizeName("OSTRACODA_INDET"), is("Ostracoda"));
    }

}
