package org.eol.globi.service;

import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterFactoryImplTest {

    @Test
    public void createWebOfLife() throws StudyImporterException {
        Class<? extends DatasetImporter> clazz = StudyImporterFactoryImpl.importerForFormat("web-of-life");
        assertThat(clazz, Is.is(not(nullValue())));
    }

    @Test
    public void createImporter() throws StudyImporterException {
        DatasetImporter someImporter = new StudyImporterFactoryImpl(null)
                .createImporter(new DatasetWithResourceMapping("namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop())));

        assertThat(someImporter, instanceOf(DatasetImporterForTSV.class));
    }

}