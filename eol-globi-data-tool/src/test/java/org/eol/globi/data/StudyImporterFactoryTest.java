package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterFactoryTest {

    @Test
    public void checkStudyImporters() throws StudyImporterException {
        for (Class importer : StudyImporterFactory.getOpenImporters()) {
            assertThat("failed to instantiate [" + importer.getSimpleName() + "] found", new StudyImporterFactory(null, null).instantiateImporter(importer), is(notNullValue()));
        }
    }

}
