package org.eol.globi.tool;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CmdGenerateReportNeo4j2Test extends GraphDBNeo4jTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void generateIndividualStudySourceReports() throws NodeFactoryException, IOException {
        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("az/source", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream)));
        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset1);
        createStudy(study2);

        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("zother/source",
                        URI.create("http://example.com"),
                        new ResourceServiceLocalAndRemote(inStream -> inStream)));

        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        getCmdGenerateReport().generateReportForSourceIndividuals();

        String escapedQuery = QueryParser.escape("globi:az/source");
        IndexHits<Node> reports = getGraphDb()
                .index()
                .forNodes("reports")
                .query(StudyConstant.SOURCE_ID, escapedQuery);

        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("globi:az/source"));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb()
                .index()
                .forNodes("reports")
                .query(StudyConstant.SOURCE_ID, "globi\\:zother\\/source");

        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother/source"));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
    }

    @Test
    public void generateStudySourceOrganizationReports() throws NodeFactoryException, IOException {
        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("az/source1", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream)));

        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);

        Dataset originatingDataset2 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("az/source2", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream)));

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset2);
        createStudy(study2);

        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("zother/source", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream)));

        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        getCmdGenerateReport().generateReportForSourceOrganizations();

        IndexHits<Node> reports = getGraphDb()
                .index()
                .forNodes("reports")
                .query(StudyConstant.SOURCE_ID, "globi\\:az");

        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("globi:az"));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb()
                .index()
                .forNodes("reports")
                .query(StudyConstant.SOURCE_ID, "globi\\:zother");

        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother"));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException, IOException {
        DatasetImpl originatingDataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(inStream -> inStream));
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
            IndexHits<Node> reports = getGraphDb()
                    .index()
                    .forNodes("reports")
                    .query("*", "*");

            assertThat(reports.size(), is(1));
            Node reportNode = reports.getSingle();
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
            assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
            tx.success();
        }
    }

    private CmdGenerateReportNeo4j2 getCmdGenerateReport() throws IOException {
        CmdGenerateReportNeo4j2 cmdGenerateReport = new CmdGenerateReportNeo4j2();
        cmdGenerateReport.setCacheDir(folder.newFolder().getAbsolutePath());
        cmdGenerateReport.setNodeFactoryFactory(factory -> nodeFactory);
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