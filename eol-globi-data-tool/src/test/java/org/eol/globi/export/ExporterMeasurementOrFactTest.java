package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterMeasurementOrFactTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null, "Canis lupus", "Homo sapiens");

        String expected =
                "\nglobi:occur:stomach_volume:2,globi:occur:2,yes,,,stomach volume,666.0,http://purl.obolibrary.org/obo/UO_0000098,,,1992-03-30T08:00:00Z,,,,myStudy,,,globi:ref:1"
                        + "\nglobi:occur:volume:4,globi:occur:4,yes,,,volume,124.0,http://purl.obolibrary.org/obo/UO_0000098,,,1992-03-30T08:00:00Z,,,,myStudy,,,globi:ref:1"
                        + "\nglobi:occur:volume:6,globi:occur:6,yes,,,volume,18.0,http://purl.obolibrary.org/obo/UO_0000098,,,1992-03-30T08:00:00Z,,,,myStudy,,,globi:ref:1";


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterMeasurementOrFact().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void noMatchNames() throws IOException, NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        nodeFactory.createSpecimen(myStudy, PropertyAndValueDictionary.NO_NAME, "externalId1");
        nodeFactory.createSpecimen(myStudy, "Some namus", PropertyAndValueDictionary.NO_MATCH);

        Study myStudy1 = nodeFactory.findStudy("myStudy");
        StringWriter row = new StringWriter();
        new ExporterMeasurementOrFact().exportStudy(myStudy1, row, false);
        assertThat(row.getBuffer().toString(), equalTo(""));
    }

    protected void assertResult(String targetTaxonName, String sourceTaxonName, String expected) throws NodeFactoryException, ParseException, IOException {
        createTestData(null, targetTaxonName, sourceTaxonName);
        Study myStudy1 = nodeFactory.findStudy("myStudy");
        StringWriter row = new StringWriter();
        new ExporterMeasurementOrFact().exportStudy(myStudy1, row, false);
        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void noMatchTargetTaxon() throws IOException, NodeFactoryException, ParseException {
        assertResult(PropertyAndValueDictionary.NO_MATCH, "Homo sapiens", "\nglobi:occur:stomach_volume:2,globi:occur:2,yes,,,stomach volume,666.0,http://purl.obolibrary.org/obo/UO_0000098,,,1992-03-30T08:00:00Z,,,,myStudy,,,globi:ref:1");
    }

    private void createTestData(Double length, String targetTaxonName, String sourceTaxonName) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen(myStudy, sourceTaxonName, "externalId1");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        Specimen otherSpecimen = nodeFactory.createSpecimen(myStudy, targetTaxonName, "externalId2");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);

        otherSpecimen = nodeFactory.createSpecimen(myStudy, targetTaxonName, "externalId2");
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(22.0, 129.9, -60.0);
        specimen.caughtIn(location);
        resolveNames();
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterMeasurementOrFact exporter = new ExporterMeasurementOrFact();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}