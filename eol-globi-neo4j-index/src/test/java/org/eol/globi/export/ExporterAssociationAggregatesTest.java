package org.eol.globi.export;

import org.eol.globi.data.GraphDBNeo4j2TestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
public class ExporterAssociationAggregatesTest extends GraphDBNeo4j2TestCase {

    private Taxon setPathAndId(TaxonImpl taxon) {
        taxon.setExternalId(taxon.getName() + "id");
        taxon.setPath(taxon.getName() + "path");
        return taxon;
    }

    @Before
    public void setEnricher() {
        taxonIndex = new NonResolvingTaxonIndexNeo4j2(getGraphDb());
    }

    @Test
    public void exportCSVNoHeader() throws IOException, NodeFactoryException, ParseException {
        String[] studyTitles = {"myStudy1", "myStudy2"};

        for (String studyTitle : studyTitles) {
            createTestData(null, studyTitle);
        }
        resolveNames();

        nodeFactory.findStudy(new StudyImpl("myStudy1")).setExternalId("some:id");

        ExporterAssociationAggregates exporter = new ExporterAssociationAggregates();
        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy(new StudyImpl("myStudy1"));
        StringWriter row = new StringWriter();
        exporter.exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), true);

        String expected1 = "associationID\toccurrenceID\tassociationType\ttargetOccurrenceID\tmeasurementDeterminedDate\tmeasurementDeterminedBy\tmeasurementMethod\tmeasurementRemarks\tsource\tbibliographicCitation\tcontributor\treferenceID";
        String expected2 = "globi:assoc:X-Homo sapiensid-ATE-Canis lupusid\tglobi:occur:source:X-Homo sapiensid-ATE\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:target:X-Homo sapiensid-ATE-Canis lupusid\t\t\t\t\tcontributor. pubYear. description\t\t\tglobi:ref:X";
        String expected3 = "globi:assoc:X-Homo sapiensid-ATE-Canis lupusid\tglobi:occur:source:X-Homo sapiensid-ATE\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:target:X-Homo sapiensid-ATE-Canis lupusid\t\t\t\t\tcontributor. pubYear. description\t\t\tglobi:ref:X";
        String actual = row.getBuffer().toString();
        assertThat(actual, startsWith(expected1));
        ExportTestUtil.assertSameAsideFromNodeIds(actual.split("\\n"), new String[] {expected1, expected2, expected3});
    }

    @Test
    public void exportNoMatchTaxa() throws IOException, NodeFactoryException, ParseException {
        String[] studyTitles = {"myStudy1", "myStudy2"};

        for (String studyTitle : studyTitles) {
            Study myStudy = nodeFactory.getOrCreateStudy(new StudyImpl(studyTitle, null, ExternalIdUtil.toCitation("contributor", "description", "pubYear")));
            Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, null));
            specimen.ate(nodeFactory.createSpecimen(myStudy, new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, null)));
        }
        resolveNames();

        ExporterAssociationAggregates exporter = new ExporterAssociationAggregates();
        StringWriter row = new StringWriter();
        for (String studyTitle : studyTitles) {
            StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy(new StudyImpl(studyTitle));
            exporter.exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), false);
        }


        assertThat(row.getBuffer().toString(), equalTo(""));
    }

    private void createTestData(Double length, String studyTitle) throws NodeFactoryException {
        Study myStudy = nodeFactory.getOrCreateStudy(new StudyImpl(studyTitle, null, ExternalIdUtil.toCitation("contributor", "description", "pubYear")));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, setPathAndId(new TaxonImpl("Homo sapiens", null)));
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GlOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GlOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        TaxonImpl taxon = new TaxonImpl("Canis lupus", null);
        Specimen otherSpecimen = nodeFactory.createSpecimen(myStudy, setPathAndId(taxon));
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(44.0, 120.0, -60.0, null));
        specimen.caughtIn(location);
    }

}
