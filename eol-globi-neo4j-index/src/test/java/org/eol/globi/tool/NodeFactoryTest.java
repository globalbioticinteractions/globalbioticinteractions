package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForSimons;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryNeo4j3;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterTestFactory;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.export.GraphExporterImpl;
import org.eol.globi.taxon.NonResolvingTaxonIndexNoTxNeo4j3;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.cache.ProvenancePathFactoryImpl;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
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

public class NodeFactoryTest extends GraphDBNeo4jTestCase {

    private final static Logger LOG = LoggerFactory.getLogger(NodeFactoryTest.class);

    @Test
    public void doSingleImportNeo4j2() throws StudyImporterException {
        GraphDatabaseService graphDb = getGraphDb();
        NodeFactory factory = createNeo4j2(graphDb);

        try (Transaction tx = getGraphDb().beginTx()) {
            importWithGraphDB(factory);
            tx.commit();
        }
    }

    private NodeFactory createNeo4j2(GraphDatabaseService graphDb) {
        NodeFactory factory;
        try (Transaction tx = getGraphDb().beginTx()) {
            factory = new NodeFactoryNeo4j3(graphDb, getCacheDir());
            tx.commit();
        }
        return factory;
    }

    private void importWithGraphDB(NodeFactory factory) throws StudyImporterException {

        importData(DatasetImporterForSimons.class, factory);

        GraphDatabaseService graphService = getGraphDb();
        Study study = getStudySingleton(graphService);
        assertThat(study.getTitle(), is("Simons 1997"));

        try (Transaction transaction = graphService.beginTx()) {
            assertNotNull(transaction.getNodeById(1));
            assertNotNull(transaction.getNodeById(200));
            transaction.commit();
        }
    }

    @Test
    public void doSingleImportNeo4j3() throws StudyImporterException {

        try (Transaction tx = getGraphDb().beginTx()) {
            NodeFactoryNeo4j3.initSchema(getGraphDb());
            tx.commit();
        }

        NodeFactoryNeo4j factory = new NodeFactoryNeo4j3(getGraphDb(), getCacheDir());
        try (Transaction tx = getGraphDb().beginTx()) {
            assertGraphDBImportNativeIndexes(factory, getGraphDb());
        }
    }

    public static void assertGraphDBImportNativeIndexes(NodeFactoryNeo4j factory, GraphDatabaseService graphDb) throws StudyImporterException {
        importData(DatasetImporterForSimons.class, factory);

        try (Transaction transaction = graphDb.beginTx()) {
            ResourceIterator<Node> nodes = transaction.findNodes(NodeLabel.Reference);
            transaction.commit();
            assertTrue(nodes.hasNext());

            Study study = new StudyNode(nodes.next());
            assertThat(study.getTitle(), is("Simons 1997"));

            assertFalse(nodes.hasNext());
            assertNotNull(transaction.getNodeById(1));
            assertNotNull(transaction.getNodeById(200));
            transaction.commit();
        }
    }

    @Test
    public void doSingleImportExportV2() throws StudyImporterException, URISyntaxException {
        createNeo4j2(getGraphDb());
        doSingleImportExport(new NodeFactoryFactoryTransactingOnDatasetNeo4j3(getGraphFactory()));
    }

    @Test
    public void doSingleImportExportV3() throws StudyImporterException, URISyntaxException {
        doSingleImportExport(new NodeFactoryFactoryTransactingOnDatasetNeo4j3(getGraphFactory()));
    }

    public void doSingleImportExport(NodeFactoryFactory nodeFactoryFactory) throws URISyntaxException, StudyImporterException {
        URL resource = getClass().getResource("datasets-test/globalbioticinteractions/template-dataset/access.tsv");
        assertNotNull(resource);
        String datasetDirTest = new File(resource.toURI()).getParentFile().getParentFile().getParentFile().getAbsolutePath();

        final IndexerDataset indexerDataset = new IndexerDataset(
                DatasetRegistryUtil.getDatasetRegistry(
                        datasetDirTest,
                        new ResourceServiceLocal(new InputStreamFactoryNoop()),
                        new ContentPathFactoryDepth0(),
                        new ProvenancePathFactoryImpl(),
                        datasetDirTest
                ),
                nodeFactoryFactory,
                getGraphFactory(),
                getCacheDir()
        );

        try (Transaction tx = getGraphDb().beginTx()) {
            indexerDataset.index();
            new NonResolvingTaxonIndexNoTxNeo4j3(getGraphDb()).findTaxonByName("bla");
            tx.commit();
        }


        try (Transaction tx = getGraphDb().beginTx()) {
            indexerDataset.index();
            tx.commit();
        }

        File baseDir = new File("target/normalizer-test/");
        FileUtils.deleteQuietly(baseDir);
        try (Transaction tx = getGraphDb().beginTx()) {
            new GraphExporterImpl()
                    .export(getGraphDb(), baseDir, "2");
            tx.commit();
        }
    }

    private static void importData(Class<? extends DatasetImporter> importer,
                                   NodeFactory factory) throws StudyImporterException {
        DatasetImporter datasetImporter = createStudyImporter(importer, factory);
        LOG.info("[" + importer + "] importing ...");
        datasetImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
    }

    private static DatasetImporter createStudyImporter(
            Class<? extends DatasetImporter> studyImporter,
            NodeFactory factory) throws StudyImporterException {
        DatasetImporter importer = new StudyImporterTestFactory(factory).instantiateImporter(studyImporter);
        importer.setLogger(new NullImportLogger());
        return importer;
    }

}