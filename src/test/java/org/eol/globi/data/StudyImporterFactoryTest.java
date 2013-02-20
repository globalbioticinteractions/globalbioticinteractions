package org.eol.globi.data;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterFactoryTest {

    @Test
    public void checkStudyImporters() {
        StudyLibrary.Study[] values = StudyLibrary.Study.values();
        for (StudyLibrary.Study value : values) {
            assertThat("no studyimport for [" + value.toString() + "] found", new StudyImporterFactory(null, null).createImporterForStudy(value), is(notNullValue()));
        }
    }

}
