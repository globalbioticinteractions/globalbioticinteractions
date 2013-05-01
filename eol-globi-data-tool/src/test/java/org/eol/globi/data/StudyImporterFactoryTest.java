package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterFactoryTest {

    @Test
    public void checkStudyImporters() throws StudyImporterException {
        for (Class importer : StudyImporterFactory.IMPORTERS) {
            assertThat("failed to instantiate [" + importer.getSimpleName() + "] found", new StudyImporterFactory(null, null).createImporterForStudy(importer), is(notNullValue()));
        }
    }

}
