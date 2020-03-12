package org.eol.globi.service;

import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForTSV;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class StudyImporterFactoryImplTest {

    @Test
    public void createWebOfLife() throws StudyImporterException {
        Class<? extends StudyImporter> clazz = StudyImporterFactoryImpl.importerForFormat("web-of-life");
        assertThat(clazz, Is.is(not(nullValue())));
    }

    @Test
    public void createImporter() throws StudyImporterException {
        StudyImporter someImporter = new StudyImporterFactoryImpl(null)
                .createImporter(new DatasetImpl("namespace", URI.create("some:uri"), is -> is));

        assertThat(someImporter, instanceOf(StudyImporterForTSV.class));
    }

}