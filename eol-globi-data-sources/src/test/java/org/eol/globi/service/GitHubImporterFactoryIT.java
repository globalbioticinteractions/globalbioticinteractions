package org.eol.globi.service;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGoMexSI;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class GitHubImporterFactoryIT {

    @Test
    public void createGoMexSI() throws URISyntaxException, NodeFactoryException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("gomexsi/interaction-data", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForGoMexSI.class)));
        StudyImporterForGoMexSI gomexsiImporter = (StudyImporterForGoMexSI) importer;
        assertThat(gomexsiImporter.getSourceCitation(), is("http://gomexsi.tamucc.edu"));
    }

}