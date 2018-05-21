package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReportGeneratorTest extends GraphDBTestCase {


    @Test
    public void generateStudyReport() throws NodeFactoryException {
        StudyImpl study1 = new StudyImpl("a second title", "a third source", null, null);
        study1.setSourceId("a/third/source");
        createStudy(study1);

        StudyImpl studyWithDoi = new StudyImpl("a title", "a third source", "doi:12345", "citation");
        studyWithDoi.setSourceId("a/third/source");
        createStudy(studyWithDoi);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForStudies();

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.TITLE, "a title");
        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat((String) reportNode.getProperty(StudyConstant.TITLE), is("a title"));
        assertThat((String) reportNode.getProperty(StudyConstant.SOURCE_ID), is("a/third/source"));
        assertThat((String) reportNode.getProperty(StudyConstant.CITATION), is("citation"));
        assertThat((String) reportNode.getProperty(StudyConstant.DOI), is("doi:12345"));
        assertThat((String) reportNode.getProperty(PropertyAndValueDictionary.EXTERNAL_ID), is("https://doi.org/12345"));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        reports.close();

        reports = getGraphDb().index().forNodes("reports").get(StudyConstant.TITLE, "a second title");
        assertThat(reports.size(), is(1));
        reportNode = reports.getSingle();
        assertThat((String) reportNode.getProperty(StudyConstant.TITLE), is("a second title"));
        assertThat((String) reportNode.getProperty(StudyConstant.SOURCE_ID), is("a/third/source"));
        assertThat(reportNode.hasProperty(StudyConstant.CITATION), is(false));
        assertThat(reportNode.hasProperty(StudyConstant.DOI), is(false));
        assertThat(reportNode.hasProperty(PropertyAndValueDictionary.EXTERNAL_ID), is(false));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        reports.close();
    }


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

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "az/source");
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat((String) reportNode.getProperty(StudyConstant.SOURCE_ID), is("az/source"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "zother/source");
        Node otherReport = otherReports.getSingle();
        assertThat((String) otherReport.getProperty(StudyConstant.SOURCE_ID), is("zother/source"));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
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

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "az");
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat((String) reportNode.getProperty(StudyConstant.SOURCE_ID), is("az"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE_ID, "zother");
        Node otherReport = otherReports.getSingle();
        assertThat((String) otherReport.getProperty(StudyConstant.SOURCE_ID), is("zother"));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
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

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE, "az source");
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
        assertThat((String) reportNode.getProperty(StudyConstant.SOURCE), is("az source"));
        reports.close();

        IndexHits<Node> otherReports = getGraphDb().index().forNodes("reports").get(StudyConstant.SOURCE, "zother source");
        Node otherReport = otherReports.getSingle();
        assertThat((String) otherReport.getProperty(StudyConstant.SOURCE), is("zother source"));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(1));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(4));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) otherReport.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
    }

    @Test
    public void generateCollectionReport() throws NodeFactoryException {
        DatasetImpl originatingDataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        StudyImpl study1 = new StudyImpl("a title", "source", null, "citation");
        study1.setOriginatingDataset(originatingDataset);
        createStudy(study1);
        StudyImpl study2 = new StudyImpl("another title", "another source", null, "citation");
        study2.setOriginatingDataset(originatingDataset);
        createStudy(study2);
        resolveNames();

        new ReportGenerator(getGraphDb()).generateReportForCollection();

        IndexHits<Node> reports = getGraphDb().index().forNodes("reports").query("*:*");
        assertThat(reports.size(), is(1));
        Node reportNode = reports.getSingle();
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_SOURCES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DATASETS), is(1));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_STUDIES), is(2));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_INTERACTIONS), is(8));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA), is(3));
        assertThat((Integer) reportNode.getProperty(PropertyAndValueDictionary.NUMBER_OF_DISTINCT_TAXA_NO_MATCH), is(2));
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