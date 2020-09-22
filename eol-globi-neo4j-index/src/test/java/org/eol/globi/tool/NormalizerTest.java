package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForSimons;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterTestFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class NormalizerTest extends GraphDBTestCase {

    private final static Log LOG = LogFactory.getLog(NormalizerTest.class);

    @Test
    public void handleOptions() throws ParseException {
        CommandLine commandLine = Normalizer.parseOptions(new String[]{"-h"});
        assertThat(commandLine.hasOption("h"), is(true));
        commandLine = Normalizer.parseOptions(new String[]{"--help"});
        assertThat(commandLine.hasOption("h"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"--skipImport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"-skipImport", "--skipExport"});
        assertThat(commandLine.hasOption("-skipImport"), is(true));
        assertThat(commandLine.hasOption("skipImport"), is(true));
        assertThat(commandLine.hasOption("skipExport"), is(true));
        assertThat(commandLine.hasOption("skipLink"), is(false));

        commandLine = Normalizer.parseOptions(new String[]{"-skipLink", "--skipExport"});
        assertThat(commandLine.hasOption("skipLink"), is(true));

        commandLine = Normalizer.parseOptions(new String[]{"-datasetDir", "some/bla"});
        assertThat(commandLine.getOptionValue("datasetDir"), is("some/bla"));
    }

    @Test
    public void doSingleImport() throws StudyImporterException {
        importData(DatasetImporterForSimons.class, new NodeFactoryNeo4j(getGraphDb()));
        GraphDatabaseService graphService = getGraphDb();

        Study study = getStudySingleton(graphService);
        assertThat(study.getTitle(), is("Simons 1997"));

        Transaction transaction = graphService.beginTx();
        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
        transaction.success();
        transaction.close();
    }

    private Normalizer createNormalizer() {
        return new Normalizer();
    }

    @Test
    public void doSingleImportExport() throws StudyImporterException, URISyntaxException {
        Normalizer dataNormalizationTool = createNormalizer();

        URL resource = getClass().getResource("datasets-test/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        String datasetDirTest = new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
        final IndexerDataset indexerDataset = new IndexerDataset(DatasetRegistryUtil.getDatasetRegistry(datasetDirTest));
        indexerDataset.index(getGraphFactory());

        String baseDir = "./target/normalizer-test/";
        FileUtils.deleteQuietly(new File(baseDir));
        dataNormalizationTool.exportData(getGraphDb(), baseDir);
    }

    private static void importData(Class<? extends DatasetImporter> importer, NodeFactoryNeo4j factory) throws StudyImporterException {
        DatasetImporter datasetImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        datasetImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private static DatasetImporter createStudyImporter(Class<? extends DatasetImporter> studyImporter, NodeFactoryNeo4j factory) throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(factory).instantiateImporter(studyImporter);
        importer.setLogger(new NullImportLogger());
        return importer;
    }

}