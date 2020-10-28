package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.CacheService;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class ReportGeneratorTest extends GraphDBTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CacheService cacheService;

    @Before
    public void init() throws IOException {
        final CacheService cacheService = new CacheService();
        cacheService.setCacheDir(folder.newFolder());
        this.cacheService = cacheService;
    }


    @Test
    public void generateIndividualStudySourceReports() throws NodeFactoryException {

        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetImpl("az/source", URI.create("http://example.com"), inStream -> inStream));
        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);


        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset1);
        createStudy(study2);


        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetImpl("zother/source", URI.create("http://example.com"), inStream -> inStream));


        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        new ReportGenerator(getGraphDb(), cacheService).generateReportForSourceIndividuals();

        Transaction transaction = getGraphDb().beginTx();

        IndexHits<Node> reports = getGraphDb()
                .index()
                .forNodes("reports")
                .get(StudyConstant.SOURCE_ID, "globi:az/source");

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
                .get(StudyConstant.SOURCE_ID, "globi:zother/source");

        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother/source"));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));

        transaction.success();
        transaction.close();
    }

    @Test
    public void generateStudySourceOrganizationReports() throws NodeFactoryException {
        Dataset originatingDataset1 = nodeFactory.getOrCreateDataset(
                new DatasetImpl("az/source1", URI.create("http://example.com"), inStream -> inStream));

        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDataset1);
        createStudy(study1);

        Dataset originatingDataset2 = nodeFactory.getOrCreateDataset(
                new DatasetImpl("az/source2", URI.create("http://example.com"), inStream -> inStream));

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDataset2);
        createStudy(study2);

        Dataset originatingDataset3 = nodeFactory.getOrCreateDataset(
                new DatasetImpl("zother/source", URI.create("http://example.com"), inStream -> inStream));

        StudyImpl study3 = new StudyImpl("yet another title", null, null);
        study3.setOriginatingDataset(originatingDataset3);
        createStudy(study3);
        resolveNames();

        new ReportGenerator(getGraphDb(), cacheService).generateReportForSourceOrganizations();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb()
                .index()
                .forNodes("reports")
                .get(StudyConstant.SOURCE_ID, "globi:az");

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
                .get(StudyConstant.SOURCE_ID, "globi:zother");

        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("globi:zother"));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));

        transaction.success();
        transaction.close();
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException {
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        Dataset originatingDatasetNode = nodeFactory.getOrCreateDataset(originatingDataset);
        StudyImpl study1 = new StudyImpl("a title", null, "citation");
        study1.setOriginatingDataset(originatingDatasetNode);
        createStudy(study1);

        StudyImpl study2 = new StudyImpl("another title", null, "citation");
        study2.setOriginatingDataset(originatingDatasetNode);
        createStudy(study2);
        resolveNames();

        new ReportGenerator(getGraphDb(), cacheService).generateReportForCollection();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb()
                .index()
                .forNodes("reports")
                .query("*:*");

        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        transaction.success();
        transaction.close();
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