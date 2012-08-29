package org.trophic.graph.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterFactoryTest {

    @Test
    public void createImporterForAkin() throws StudyImporterException {
        StudyImporter importerForStudy = new StudyImporterFactory(null, null).createImporterForStudy(StudyLibrary.Study.AKIN_MAD_ISLAND);
        assertThat(importerForStudy, is(not(nullValue())));
        assertThat(importerForStudy, is(StudyImporterForAkin.class));
    }

    @Test
    public void createImporterForMSAL() throws StudyImporterException {
        StudyImporter importerForStudy = new StudyImporterFactory(null, null).createImporterForStudy(StudyLibrary.Study.MISSISSIPPI_ALABAMA);
        assertThat(importerForStudy, is(not(nullValue())));
        assertThat(importerForStudy, is(StudyImporterForMississippiAlabama.class));
    }

    @Test
    public void createImporterForLavaca() throws StudyImporterException {
        StudyImporter importerForStudy = new StudyImporterFactory(null, null).createImporterForStudy(StudyLibrary.Study.LACAVA_BAY);
        assertThat(importerForStudy, is(not(nullValue())));
        assertThat(importerForStudy, is(StudyImporterForLavacaBay.class));
    }
}
