package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForArthopodEasyCapture;
import org.eol.globi.data.StudyImporterForCoetzer;
import org.eol.globi.data.StudyImporterForGoMexSI2;
import org.eol.globi.data.StudyImporterForMetaTable;
import org.eol.globi.data.StudyImporterForPlanque;
import org.eol.globi.data.StudyImporterForSzoboszlai;
import org.eol.globi.data.StudyImporterForTSV;
import org.eol.globi.data.StudyImporterForWood;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GitHubImporterFactoryIT {

    @Test
    public void createGoMexSI() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("gomexsi/interaction-data", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForGoMexSI2.class)));
        StudyImporterForGoMexSI2 gomexsiImporter = (StudyImporterForGoMexSI2) importer;
        assertThat(gomexsiImporter.getSourceCitation(), is("http://gomexsi.tamucc.edu"));
    }

    @Test
    public void createSzoboszlai() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/szoboszlai2015", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForSzoboszlai.class)));
        StudyImporterForSzoboszlai importerz = (StudyImporterForSzoboszlai) importer;
        assertThat(importerz.getSourceCitation(), containsString("Szoboszlai"));
    }

    @Test
    public void createWood() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/wood2015", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForWood.class)));
        StudyImporterForWood importerz = (StudyImporterForWood) importer;
        assertThat(importerz.getSourceCitation(), containsString("Wood"));
        assertThat(importerz.getLinkResource(), is(notNullValue()));
    }

    @Test
    public void createPlanque() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/planque2014", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForPlanque.class)));
        StudyImporterForPlanque importerz = (StudyImporterForPlanque) importer;
        assertThat(importerz.getSourceCitation(), containsString("Planque"));
        assertThat(importerz.getLinks(), is(notNullValue()));
        assertThat(importerz.getReferences(), is(notNullValue()));
        assertThat(importerz.getReferencesForLinks(), is(notNullValue()));
    }

    @Test
    public void createArthopodEasyCapture() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/arthropodEasyCaptureAMNH", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForArthopodEasyCapture.class)));
        assertThat(((StudyImporterForArthopodEasyCapture)importer).getRssFeedUrlString(), is(notNullValue()));
    }

    @Test
    public void createMetaTable() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/AfricaTreeDatabase", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForMetaTable.class)));
        assertThat(((StudyImporterForMetaTable)importer).getConfig(), is(notNullValue()));
        assertThat(((StudyImporterForMetaTable)importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/"));
    }

    @Test
    public void createAfrotropicalBees() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/Catalogue-of-Afrotropical-Bees", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForCoetzer.class)));
        assertThat(((StudyImporterForCoetzer)importer).getDataset(), is(notNullValue()));
        assertThat(((StudyImporterForCoetzer)importer).getArchiveURL(), is("https://dl.dropboxusercontent.com/u/13322685/CatalogueOfAfrotropicalBees.zip"));
    }

    @Test
    public void defaultTSVImporter() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/template-dataset", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForTSV.class)));
        assertThat(((StudyImporterForTSV)importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/template-dataset/"));
    }

    @Test
    public void createMetaTableREEM() throws URISyntaxException, StudyImporterException, IOException {
        StudyImporter importer = new GitHubImporterFactory().createImporter("globalbioticinteractions/noaa-reem", null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForMetaTable.class)));
        final JsonNode config = ((StudyImporterForMetaTable) importer).getConfig();
        assertThat(config, is(notNullValue()));
    }

}