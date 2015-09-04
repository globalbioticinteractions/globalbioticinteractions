package org.eol.globi.service;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForGoMexSI;
import org.eol.globi.data.StudyImporterForPlanque;
import org.eol.globi.data.StudyImporterForSzoboszlai;
import org.eol.globi.data.StudyImporterForWood;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.StringContains.containsString;

public class GitHubImporterFactoryIT {

    @Test
    public void createGoMexSI() throws URISyntaxException, NodeFactoryException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("gomexsi/interaction-data", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForGoMexSI.class)));
        StudyImporterForGoMexSI gomexsiImporter = (StudyImporterForGoMexSI) importer;
        assertThat(gomexsiImporter.getSourceCitation(), is("http://gomexsi.tamucc.edu"));
    }

    @Test
    public void createSzoboszlai() throws URISyntaxException, NodeFactoryException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/szoboszlai2015", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForSzoboszlai.class)));
        StudyImporterForSzoboszlai importerz = (StudyImporterForSzoboszlai) importer;
        assertThat(importerz.getSourceCitation(), containsString("Szoboszlai"));
    }

    @Test
    public void createWood() throws URISyntaxException, NodeFactoryException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/wood2015", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForWood.class)));
        StudyImporterForWood importerz = (StudyImporterForWood) importer;
        assertThat(importerz.getSourceCitation(), containsString("Wood"));
        assertThat(importerz.getLinksURL(), is(notNullValue()));
    }

    @Test
    public void createPlanque() throws URISyntaxException, NodeFactoryException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/planque2014", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForPlanque.class)));
        StudyImporterForPlanque importerz = (StudyImporterForPlanque) importer;
        assertThat(importerz.getSourceCitation(), containsString("Planque"));
        assertThat(importerz.getLinks(), is(notNullValue()));
        assertThat(importerz.getReferences(), is(notNullValue()));
        assertThat(importerz.getReferencesForLinks(), is(notNullValue()));
    }

}