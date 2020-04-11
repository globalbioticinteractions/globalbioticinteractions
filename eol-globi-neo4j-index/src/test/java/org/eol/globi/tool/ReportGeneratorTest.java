package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;

import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReportGeneratorTest extends GraphDBTestCase {

    @Test
    public void generateIndividualStudySourceReports() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("a title", "az source", null, "citation");
        study1.setSourceId("az/source");
        createStudy(study1);
        StudyImpl study2 = new StudyImpl("another title", "az source", null, "citation");
        study2.setSourceId("az/source");
        createStudy(study2);
        StudyImpl study3 = new StudyImpl("yet another title", "zother source", null, null);
        study3.setSourceId("zother/source");
        createStudy(study3);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForSourceIndividuals();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "az/source");
        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("az/source"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "zother/source");
        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("zother/source"));
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
        StudyImpl study1 = new StudyImpl("a title", "az source", null, "citation");
        study1.setSourceId("az/source1");
        createStudy(study1);
        StudyImpl study2 = new StudyImpl("another title", "az source", null, "citation");
        study2.setSourceId("az/source2");
        createStudy(study2);
        StudyImpl study3 = new StudyImpl("yet another title", "zother source", null, null);
        study3.setSourceId("zother/source");
        createStudy(study3);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForSourceOrganizations();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "az");
        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat(reportNode.getProperty(StudyConstant.SOURCE_ID), is("az"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "zother");
        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE_ID), is("zother"));
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
    public void generateStudySourceCitationReports() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("a title", "az source", null, "citation");
        study1.setSourceId("az/source1");
        createStudy(study1);
        StudyImpl study2 = new StudyImpl("another title", "az source", null, "citation");
        study2.setSourceId("az/source2");
        createStudy(study2);
        StudyImpl study3 = new StudyImpl("yet another title", "zother source", null, null);
        study3.setSourceId("zother/source");
        createStudy(study3);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForSourceCitations();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE, "az source");
        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat(reportNode.getProperty(StudyConstant.SOURCE), is("az source"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE, "zother source");
        Node otherReport = otherReports.getSingle();
        assertThat(otherReport.getProperty(StudyConstant.SOURCE), is("zother source"));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat(otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));

        transaction.success();
        transaction.close();
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException {
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        StudyImpl study1 = new StudyImpl("a title", "source", null, "citation");
        study1.setOriginatingDataset(originatingDataset);
        createStudy(study1);
        StudyImpl study2 = new StudyImpl("another title", "another source", null, "citation");
        study2.setOriginatingDataset(originatingDataset);
        createStudy(study2);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForCollection();

        Transaction transaction = getGraphDb().beginTx();
        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").query("*:*");
        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat(reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
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