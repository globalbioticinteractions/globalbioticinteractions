package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CmdGenerateReportTest extends GraphDBTestCase {

    @Test
    public void generateIndividualStudySourceReports() throws NodeFactoryException, IOException {
        assertIndividualDatasetReports("az/source");
    }

    @Test
    public void generateIndividualStudySourceReportsColon() throws NodeFactoryException, IOException {
        assertIndividualDatasetReports("urn:lsid:checklistbank.org:dataset:1234");
    }

    private void assertIndividualDatasetReports(String namespace) throws NodeFactoryException, IOException {
        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping(namespace, URI.create("http://example.com"), getResourceService()));
        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset1);
        createStudy(study2);

        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("zother/source",
                        URI.create("http://example.com"),
                        getResourceService()));

        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        getCmdGenerateReport().generateReportForSourceIndividuals();

        try (Transaction tx = getGraphDb().beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Report);
            assertThat(nodes.hasNext(), Is.is(true));
            Node reportNode = nodes.next();
            assertThat(reportNode, Is.is(notNullValue()));
            assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), Is.is("globi:" + namespace));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), Is.is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), Is.is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), Is.is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), Is.is(8));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), Is.is(3));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), Is.is(2));
            tx.commit();
        }

//        String escapedQuery = QueryParser.escape("globi:" + namespace);
//        IndexHits<Node> reports = getGraphDb()
//                .index()
//                .forNodes("reports")
//                .query(StudyConstant.SOURCE_ID, escapedQuery);

//        Node reportNode = reports.getSingle();
//        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("globi:" + namespace));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
//        reports.close();
//
//        IndexHits<Node> otherReports = getGraphDb()
//                .index()
//                .forNodes("reports")
//                .query(StudyConstant.SOURCE_ID, "globi\\:zother\\/source");
//
//        Node otherReport = otherReports.getSingle();
//        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother/source"));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
    }

    @Test
    public void generateStudySourceOrganizationReports() throws NodeFactoryException, IOException {
        assertOrganizationReport("az/source1", "az/source2", "zother/source");
    }

    @Test
    public void generateStudySourceOrganizationReportsColon() throws NodeFactoryException, IOException {
        assertOrganizationReport(
                "az/source1",
                "az/source2",
                "zother/urn:lsid:checklistbank:dataset:12345");
    }

    private void assertOrganizationReport(String namespace, String namespace1, String namespace2) throws NodeFactoryException, IOException {
        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping(namespace, URI.create("http://example.com"), getResourceService()));

        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);

        Dataset originatingDataset2 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping(namespace1, URI.create("http://example.com"), getResourceService()));

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset2);
        createStudy(study2);

        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping(namespace2, URI.create("http://example.com"), getResourceService()));

        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        getCmdGenerateReport().generateReportForSourceOrganizations();

        try (Transaction tx = getGraphDb().beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Report, StudyConstant.SOURCE_ID, "globi:az");
            assertThat(nodes.hasNext(), is(true));
            Node reportNode = nodes.next();
            assertThat(nodes.hasNext(), is(false));
            assertThat(reportNode, is(notNullValue()));
            assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("globi:az"));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        }

//        IndexHits<Node> reports = getGraphDb()
//                .index()
//                .forNodes("reports")
//                .query(StudyConstant.SOURCE_ID, "globi\\:az");
//
//        Node reportNode = reports.getSingle();
//        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("globi:az"));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
//        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
//        reports.close();

//        IndexHits<Node> otherReports = getGraphDb()
//                .index()
//                .forNodes("reports")
//                .query(StudyConstant.SOURCE_ID, "globi\\:zother");
//
//        Node otherReport = otherReports.getSingle();
//        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother"));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
//        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException, IOException {
        DatasetImpl originatingDataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), getResourceService());
        Dataset originatingDatasetNode = nodeFactory.getOrCreateDataset(originatingDataset);
        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDatasetNode);
        createStudy(study1);

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDatasetNode);
        createStudy(study2);
        resolveNames();


        getCmdGenerateReport()
                .generateReportForCollection();

        try (Transaction tx = getGraphDb().beginTx()) {
            ResourceIterator<Node> nodes = tx.findNodes(NodeLabel.Report);
            assertThat(nodes.hasNext(), is(true));
            Node reportNode = nodes.next();
            assertThat(reportNode, is(notNullValue()));
            assertThat(nodes.hasNext(), is(false));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
            tx.commit();

//            IndexHits<Node> reports = getGraphDb()
//                    .index()
//                    .forNodes("reports")
//                    .query("*", "*");
//
//            assertThat(reports.size(), is(1));
//            Node reportNode = reports.getSingle();
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
//            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
//            tx.success();
        }
    }

    private CmdGenerateReport getCmdGenerateReport() throws IOException {
        CmdGenerateReport cmdGenerateReport = new CmdGenerateReport();
        final File cacheDir2 = folder.newFolder();
        cmdGenerateReport.setCacheDir(cacheDir2.getAbsolutePath());
        cmdGenerateReport.setNodeFactoryFactory((factory, cacheDir) -> nodeFactory);
        cmdGenerateReport.setGraphServiceFactory(new GraphServiceFactory() {
            @Override
            public GraphDatabaseService getGraphService() {
                return getGraphDb();
            }

            @Override
            public void close() throws Exception {

            }
        });
        return cmdGenerateReport;
    }

    protected Study createStudy(Study study1) throws NodeFactoryException {
        Study study = nodeFactory.getOrCreateStudy(study1);
        Specimen monkey = nodeFactory.createSpecimen(study, new TaxonImpl("Monkey"));
        monkey.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Banana")));
        monkey.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Banana")));
        monkey.ate(nodeFactory.createSpecimen(study, new TaxonImpl("Banana")));
        TaxonImpl apple = new TaxonImpl("Apple", "some:id");
        apple.setPath("some | path");
        monkey.ate(nodeFactory.createSpecimen(study, apple));
        return study;
    }

}