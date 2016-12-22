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

import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GitHubImporterFactoryIT {

    @Test
    public void createGoMexSI() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("gomexsi/interaction-data", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForGoMexSI2.class)));
        StudyImporterForGoMexSI2 gomexsiImporter = (StudyImporterForGoMexSI2) importer;
        assertThat(gomexsiImporter.getSourceCitation(), is("http://gomexsi.tamucc.edu"));
    }

    @Test
    public void createSzoboszlai() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/szoboszlai2015", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForSzoboszlai.class)));
        StudyImporterForSzoboszlai importerz = (StudyImporterForSzoboszlai) importer;
        assertThat(importerz.getSourceCitation(), containsString("Szoboszlai"));
    }

    @Test
    public void createWood() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/wood2015", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForWood.class)));
        StudyImporterForWood importerz = (StudyImporterForWood) importer;
        assertThat(importerz.getSourceCitation(), containsString("Wood"));
        assertThat(importerz.getLinkResource(), is(notNullValue()));
    }

    @Test
    public void createPlanque() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/planque2014", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForPlanque.class)));
        StudyImporterForPlanque importerz = (StudyImporterForPlanque) importer;
        assertThat(importerz.getSourceCitation(), containsString("Planque"));
        assertThat(importerz.getLinks(), is(notNullValue()));
        assertThat(importerz.getReferences(), is(notNullValue()));
        assertThat(importerz.getReferencesForLinks(), is(notNullValue()));
    }

    @Test
    public void createArthopodEasyCapture() throws StudyImporterException, DatasetFinderException {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/arthropodEasyCaptureAMNH", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForArthopodEasyCapture.class)));
        assertThat(((StudyImporterForArthopodEasyCapture)importer).getRssFeedUrlString(), is(notNullValue()));
    }

    @Test
    public void createMetaTable() throws DatasetFinderException, StudyImporterException {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/AfricaTreeDatabase", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForMetaTable.class)));
        assertThat(((StudyImporterForMetaTable)importer).getConfig(), is(notNullValue()));
        assertThat(((StudyImporterForMetaTable)importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/"));
    }

    @Test
    public void createAfrotropicalBees() throws StudyImporterException, DatasetFinderException {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/Catalogue-of-Afrotropical-Bees", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForCoetzer.class)));
        assertThat(((StudyImporterForCoetzer)importer).getDataset(), is(notNullValue()));
        String archiveURL = ((StudyImporterForCoetzer) importer).getArchiveURL();
        assertThat(archiveURL, endsWith("CatalogueOfAfrotropicalBees.zip"));
        assertThat(URI.create(archiveURL).isAbsolute(), is(true));
    }

    @Test
    public void defaultTSVImporter() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/template-dataset", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForTSV.class)));
        assertThat(((StudyImporterForTSV)importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/template-dataset/"));
    }

    @Test
    public void createMetaTableREEM() throws StudyImporterException, DatasetFinderException  {
        final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
        StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor("globalbioticinteractions/noaa-reem", datasetFinderGitHubRemote), null, null);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(StudyImporterForMetaTable.class)));
        final JsonNode config = ((StudyImporterForMetaTable) importer).getConfig();
        assertThat(config, is(notNullValue()));
    }

}