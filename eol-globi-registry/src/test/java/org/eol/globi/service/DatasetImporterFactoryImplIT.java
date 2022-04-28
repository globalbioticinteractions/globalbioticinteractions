package org.eol.globi.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.data.BaseDatasetImporter;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.DatasetImporterForRSS;
import org.eol.globi.data.DatasetImporterForCoetzer;
import org.eol.globi.data.DatasetImporterForGoMexSI2;
import org.eol.globi.data.DatasetImporterForHafner;
import org.eol.globi.data.DatasetImporterForHurlbert;
import org.eol.globi.data.DatasetImporterForJSONLD;
import org.eol.globi.data.DatasetImporterForMetaTable;
import org.eol.globi.data.DatasetImporterForPlanque;
import org.eol.globi.data.DatasetImporterForSzoboszlai;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.DatasetImporterForWood;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DatasetFactory;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryGitHubArchive;
import org.globalbioticinteractions.dataset.DatasetRegistryGitHubRemote;
import org.globalbioticinteractions.dataset.DatasetRegistryWithCache;
import org.globalbioticinteractions.dataset.DatasetRegistryZenodo;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class DatasetImporterFactoryImplIT {

    @Test
    public void createGoMexSI() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "gomexsi/interaction-data");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForGoMexSI2.class)));
        DatasetImporterForGoMexSI2 gomexsiImporter = (DatasetImporterForGoMexSI2) importer;
        assertThat(gomexsiImporter.getSourceCitation(), is("http://gomexsi.tamucc.edu"));
    }

    @Test
    public void createHafner() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistry datasetRegistryGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        Dataset dataset = new DatasetFactory(datasetRegistryGitHubRemote).datasetFor("globalbioticinteractions/hafner");
        DatasetImporter importer = new StudyImporterFactoryImpl(null).createImporter(dataset);
        assertThat(importer, is(notNullValue()));
        DatasetImporterForHafner haftnerImporter = (DatasetImporterForHafner) importer;
        assertThat(haftnerImporter.getDataset().retrieve(URI.create("hafner/gopher_lice_int.csv")), is(notNullValue()));
    }

    @Test
    public void createSzoboszlai() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/szoboszlai2015");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForSzoboszlai.class)));
        DatasetImporterForSzoboszlai importerz = (DatasetImporterForSzoboszlai) importer;
        assertThat(importerz.getSourceCitation(), containsString("Szoboszlai"));
    }

    @Test
    public void createWood() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/wood2015");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForWood.class)));
        DatasetImporterForWood importerz = (DatasetImporterForWood) importer;
        assertThat(importerz.getSourceCitation(), containsString("Wood"));
        assertThat(importerz.getDataset().retrieve(URI.create("links")).toString(), is(notNullValue()));
    }

    @Test
    public void createPlanque() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/planque2014");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForPlanque.class)));
        DatasetImporterForPlanque importerz = (DatasetImporterForPlanque) importer;
        assertThat(importerz.getSourceCitation(), containsString("Planque"));
    }

    @Test
    public void createArthopodEasyCapture() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/arthropodEasyCaptureAMNH");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForRSS.class)));
        assertThat(((DatasetImporterForRSS) importer).getRssFeedUrlString(), is(notNullValue()));
    }

    @Test
    public void createMetaTable() throws DatasetRegistryException, StudyImporterException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/AfricaTreeDatabase");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForMetaTable.class)));
        assertThat(((DatasetImporterForMetaTable) importer).getConfig(), is(notNullValue()));
        assertThat(((DatasetImporterForMetaTable) importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/"));
    }

    @Test
    public void createAfrotropicalBees() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        String repo = "globalbioticinteractions/Catalogue-of-Afrotropical-Bees";
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, repo);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForCoetzer.class)));
        assertThat(((DatasetImporterForCoetzer) importer).getDataset(), is(notNullValue()));
        assertThat(((DatasetImporterForCoetzer) importer).getDataset().retrieve(URI.create("archive")), is(notNullValue()));

    }

    public DatasetImporter importerFor(DatasetRegistryGitHubRemote datasetFinderGitHubRemote, String repo) throws StudyImporterException, DatasetRegistryException {
        Dataset dataset = new DatasetFactory(datasetFinderGitHubRemote).datasetFor(repo);
        return new StudyImporterFactoryImpl(null).createImporter(dataset);
    }

    @Test
    public void defaultTSVImporterCached() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistry datasetRegistry = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(inStream -> inStream)), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), "target/datasets", inStream -> inStream));
        DatasetImporter importer = getTemplateImporter(datasetRegistry, "globalbioticinteractions/template-dataset");
        DatasetImporterForTSV importerTSV = (DatasetImporterForTSV) importer;
        assertThat(importerTSV.getBaseUrl(), startsWith("https://github.com/globalbioticinteractions/template-dataset/"));
        assertThat(importerTSV.getDataset().retrieve(URI.create("globi.json")), is(notNullValue()));

    }

    @Test
    public void jsonldImporterCached() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistry datasetRegistry = new DatasetRegistryWithCache(new DatasetRegistryGitHubArchive(new ResourceServiceHTTP(inStream -> inStream)), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), "target/datasets", inStream -> inStream));
        Dataset dataset = new DatasetFactory(datasetRegistry).datasetFor("globalbioticinteractions/jsonld-template-dataset");
        DatasetImporter importer = new StudyImporterFactoryImpl(null).createImporter(dataset);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForJSONLD.class)));
    }

    @Test
    public void defaultTSVImporterCachedZenodo() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistry datasetRegistry = new DatasetRegistryWithCache(new DatasetRegistryZenodo(new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, inStream -> inStream);
            }
        }), dataset -> CacheUtil.cacheFor(dataset.getNamespace(), "target/datasets", inStream -> inStream));
        DatasetImporter importer = getTemplateImporter(datasetRegistry, "globalbioticinteractions/template-dataset");
        DatasetImporterForTSV importerTSV = (DatasetImporterForTSV) importer;
        assertThat(importerTSV.getSourceCitation(), containsString("doi.org"));
    }

    @Test
    public void defaultTSVImporterNotCached() throws StudyImporterException, DatasetRegistryException, IOException {
        final DatasetRegistry datasetRegistry = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = getTemplateImporter(datasetRegistry, "globalbioticinteractions/template-dataset");
        assertThat(((DatasetImporterForTSV) importer).getBaseUrl(), startsWith("https://raw.githubusercontent.com/globalbioticinteractions/template-dataset/"));
        InputStream actual = ((DatasetImporterForTSV) importer).getDataset().retrieve(URI.create("globi.json"));
        assertThat(actual, is(notNullValue()));
    }

    DatasetImporter getTemplateImporter(DatasetRegistry datasetRegistry, String repo) throws DatasetRegistryException, StudyImporterException {
        Dataset dataset = new DatasetFactory(datasetRegistry).datasetFor(repo);
        DatasetImporter importer = new StudyImporterFactoryImpl(null).createImporter(dataset);
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForTSV.class)));
        return importer;
    }

    @Test
    public void createMetaTableREEM() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/noaa-reem");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForMetaTable.class)));
        final JsonNode config = ((DatasetImporterForMetaTable) importer).getConfig();
        assertThat(config, is(notNullValue()));
    }

    @Test
    public void createHurlbert() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "hurlbertlab/dietdatabase");
        assertThat(importer, is(notNullValue()));
        assertThat(importer, is(instanceOf(DatasetImporterForHurlbert.class)));
        final JsonNode config = ((BaseDatasetImporter) importer).getDataset().getConfig();
        assertThat(config, is(notNullValue()));
    }

    @Test
    public void createSIAD() throws StudyImporterException, DatasetRegistryException {
        final DatasetRegistryGitHubRemote datasetFinderGitHubRemote = new DatasetRegistryGitHubRemote(inStream -> inStream);
        DatasetImporter importer = importerFor(datasetFinderGitHubRemote, "globalbioticinteractions/siad");
        assertThat(importer, is(notNullValue()));
        Dataset dataset = ((BaseDatasetImporter) importer).getDataset();
        final JsonNode config = dataset.getConfig();
        assertThat(config, is(notNullValue()));
        assertThat(dataset.getOrDefault(DatasetConstant.SHOULD_RESOLVE_REFERENCES, "donald"), is("false"));
    }

}