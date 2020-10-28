package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
public class ExporterMeasurementOrFactTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null, "Canis lupus", "Homo sapiens");

        String expected =
                "globi:occur:stomach_volume:X\tglobi:occur:X\tyes\t\t\tstomach volume\t666.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\n" +
                        "globi:occur:volume:X\tglobi:occur:X\tyes\t\t\tvolume\t124.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\n" +
                        "globi:occur:volume:X\tglobi:occur:X\tyes\t\t\tvolume\t18.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\n";


        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterMeasurementOrFact().exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), false);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString(), expected);
    }

    @Test
    public void noMatchNames() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        nodeFactory.createSpecimen(myStudy, new TaxonImpl(PropertyAndValueDictionary.NO_NAME, "externalId1"));
        nodeFactory.createSpecimen(myStudy, new TaxonImpl("Some namus", PropertyAndValueDictionary.NO_MATCH));

        StringWriter row = new StringWriter();
        new ExporterMeasurementOrFact().exportStudy(getStudySingleton(getGraphDb()), ExportUtil.AppenderWriter.of(row), false);
        assertThat(row.getBuffer().toString(), equalTo(""));
    }

    protected void assertResult(String targetTaxonName, String sourceTaxonName, String expected) throws NodeFactoryException, ParseException, IOException {
        createTestData(null, targetTaxonName, sourceTaxonName);
        StringWriter row = new StringWriter();
        new ExporterMeasurementOrFact().exportStudy(getStudySingleton(getGraphDb()), ExportUtil.AppenderWriter.of(row), false);
        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString(), expected);
    }

    @Test
    public void noMatchTargetTaxon() throws IOException, NodeFactoryException, ParseException {
        String expected =
                "globi:occur:stomach_volume:X\tglobi:occur:X\tyes\t\t\tstomach volume\t666.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\n" +
                        "globi:occur:volume:X\tglobi:occur:X\tyes\t\t\tvolume\t124.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\nglobi:occur:volume:X\tglobi:occur:X\tyes\t\t\tvolume\t18.0\thttp://purl.obolibrary.org/obo/UO_0000098\t\t\t1992-03-30T08:00:00Z\t\t\t\tmyStudy\t\t\tglobi:ref:X\n";
        assertResult(PropertyAndValueDictionary.NO_MATCH, "Homo sapiens", expected);
    }

    private void createTestData(Double length, String targetTaxonName, String sourceTaxonName) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl(sourceTaxonName, "externalId1"));
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        Specimen otherSpecimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl(targetTaxonName, "externalId2"));
        otherSpecimen.setVolumeInMilliLiter(124.0);

        nodeFactory.setUnixEpochProperty(otherSpecimen, ExportTestUtil.utcTestDate());

        specimen.ate(otherSpecimen);

        otherSpecimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl(targetTaxonName, "externalId2"));
        nodeFactory.setUnixEpochProperty(otherSpecimen, ExportTestUtil.utcTestDate());
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(22.0, 129.9, -60.0, null));
        specimen.caughtIn(location);
        resolveNames();
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterMeasurementOrFact());
    }

}