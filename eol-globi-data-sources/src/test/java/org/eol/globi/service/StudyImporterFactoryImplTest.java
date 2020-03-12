package org.eol.globi.service;

import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class StudyImporterFactoryImplTest {

    @Test
    public void createWebOfLife() throws StudyImporterException {
        Class<? extends StudyImporter> clazz = StudyImporterFactoryImpl.importerForFormat("web-of-life");
        assertThat(clazz, Is.is(not(nullValue())));
    }

}