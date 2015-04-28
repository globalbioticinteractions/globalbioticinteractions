package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
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

public class ExporterOccurrencesTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = getExpectedHeader();
        expected += getExpectedData();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString().trim(), equalTo(expected.trim()));
    }

    private String getExpectedData() {
        return "\nglobi:occur:3,EOL:327955,,,,,JUVENILE,,,,,,,,,,,,,1992-03-30T08:00:00Z,,,12.0,-1.0,,,-60.0 m,DIGESTATE,BONE" +
                "\nglobi:occur:8,EOL:328607,,,,,,,,,,,,,,,,,,1992-03-30T08:00:00Z,,,12.0,-1.0,,,-60.0 m,," +
                "\nglobi:occur:10,EOL:328607,,,,,,,,,,,,,,,,,,1992-03-30T08:00:00Z,,,12.0,-1.0,,,-60.0 m,,";
    }

    private String getExpectedHeader() {
        return "\"occurrenceID\",\"taxonID\",\"institutionCode\",\"collectionCode\",\"catalogNumber\",\"sex\",\"lifeStage\",\"reproductiveCondition\",\"behavior\",\"establishmentMeans\",\"occurrenceRemarks\",\"individualCount\",\"preparations\",\"fieldNotes\",\"basisOfRecord\",\"samplingProtocol\",\"samplingEffort\",\"identifiedBy\",\"dateIdentified\",\"eventDate\",\"modified\",\"locality\",\"decimalLatitude\",\"decimalLongitude\",\"verbatimLatitude\",\"verbatimLongitude\",\"verbatimElevation\",\"physiologicalState\",\"bodyPart\"";
    }

    @Test
    public void exportNoHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = getExpectedData();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private ExporterOccurrences exportOccurrences() {
        return new ExporterOccurrences();
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        String expected = "";
        expected += getExpectedHeader();
        expected += getExpectedData();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));

    }

    @Test
    public void dontExportToCSVSpecimenEmptyStomach() throws NodeFactoryException, IOException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen(myStudy, "Homo sapiens", "EOL:123");
        specimen.setBasisOfRecord(new Term("test:123", "aBasisOfRecord"));

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "\nglobi:occur:3,EOL:123,,,,,,,,,,,,,aBasisOfRecord,,,,,,,,,,,,,,";

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen(myStudy, "Homo sapiens", "EOL:327955");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(12.0, -1.0, -60.0);
        specimen.caughtIn(location);
        Specimen wolf1 = eatWolf(specimen, myStudy);
        wolf1.caughtIn(location);
        Specimen wolf2 = eatWolf(specimen, myStudy);
        wolf2.caughtIn(location);
    }

    private Specimen eatWolf(Specimen specimen, Study study) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, "Canis lupus", "EOL:328607");
        otherSpecimen.setVolumeInMilliLiter(124.0);
        nodeFactory.setUnixEpochProperty(otherSpecimen, ExportTestUtil.utcTestDate());
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExporterOccurrences exporter = exportOccurrences();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}
