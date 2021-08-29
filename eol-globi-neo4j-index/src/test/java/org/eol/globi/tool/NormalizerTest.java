package org.eol.globi.tool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForSimons;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.data.NodeFactoryNeo4j3;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterTestFactory;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class NormalizerTest extends GraphDBTestCase {

    private final static Logger LOG = LoggerFactory.getLogger(NormalizerTest.class);

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
    public void doSingleImportNeo4j2() throws StudyImporterException {
        importWithGraphDB(new NodeFactoryNeo4j2(getGraphDb()));
    }

    public void importWithGraphDB(NodeFactoryNeo4j factory) throws StudyImporterException {

        importData(DatasetImporterForSimons.class, factory);

        GraphDatabaseService graphService = getGraphDb();
        Study study = getStudySingleton(graphService);
        assertThat(study.getTitle(), is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    @Test
    public void doSingleImportNeo4j3() throws StudyImporterException {
        NodeFactoryNeo4j3.initSchema(getGraphDb());
        NodeFactoryNeo4j factory = new NodeFactoryNeo4j3(getGraphDb());
        assertGraphDBImportNativeIndexes(factory);
    }

    public void assertGraphDBImportNativeIndexes(NodeFactoryNeo4j factory) throws StudyImporterException {
        importData(DatasetImporterForSimons.class, factory);
        GraphDatabaseService graphService = getGraphDb();

        ResourceIterator<Node> nodes = graphService.findNodes(NodeLabel.Reference);

        assertTrue(nodes.hasNext());

        Study study = new StudyNode(nodes.next());
        assertThat(study.getTitle(), is("Simons 1997"));

        assertFalse(nodes.hasNext());
        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

    private Normalizer createNormalizer() {
        return new Normalizer();
    }

    @Test
    public void doSingleImportExportV2() throws StudyImporterException, URISyntaxException {
        doSingleImportExport(NodeFactoryNeo4j2::new);
    }

    @Test
    public void doSingleImportExportV3() throws StudyImporterException, URISyntaxException {
        doSingleImportExport(NodeFactoryNeo4j3::new);
    }

    public void doSingleImportExport(NodeFactoryFactory nodeFactoryFactory) throws URISyntaxException, StudyImporterException {
        Normalizer dataNormalizationTool = createNormalizer();

        URL resource = getClass().getResource("datasets-test/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        String datasetDirTest = new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();
        final IndexerDataset indexerDataset = new IndexerDataset(
                DatasetRegistryUtil.getDatasetRegistry(datasetDirTest),
                nodeFactoryFactory);

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