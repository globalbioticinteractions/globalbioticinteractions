package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

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
        return "\nglobi:occur:3,EOL:327955,myStudy,,,,,JUVENILE,,,,,,,,,,,,1992-03-30T08:00:00Z,,,123.0,345.9,,,-60.0 m,DIGESTATE,BONE" +
               "\nglobi:occur:6,EOL:328607,myStudy,,,,,,,,,,,,,,,,,1992-03-30T08:00:00Z,,,123.0,345.9,,,-60.0 m,," +
               "\nglobi:occur:8,EOL:328607,myStudy,,,,,,,,,,,,,,,,,1992-03-30T08:00:00Z,,,123.0,345.9,,,-60.0 m,,";
    }

    private String getExpectedHeader() {
        String header = "\"occurrenceID\",\"taxonID\",\"eventID\",\"institutionCode\",\"collectionCode\",\"catalogNumber\",\"sex\",\"lifeStage\",\"reproductiveCondition\",\"behavior\",\"establishmentMeans\",\"occurrenceRemarks\",\"individualCount\",\"preparations\",\"fieldNotes\",\"samplingProtocol\",\"samplingEffort\",\"identifiedBy\",\"dateIdentified\",\"eventDate\",\"modified\",\"locality\",\"decimalLatitude\",\"decimalLongitude\",\"verbatimLatitude\",\"verbatimLongitude\",\"verbatimElevation\",\"physiologicalState\",\"bodyPart\"" +
                "\n\"http://rs.tdwg.org/dwc/terms/occurrenceID\"," +
                "\"http://rs.tdwg.org/dwc/terms/taxonID\"," +
                "\"http://rs.tdwg.org/dwc/terms/eventID\"," +
                "\"http://rs.tdwg.org/dwc/terms/institutionCode\"," +
                "\"http://rs.tdwg.org/dwc/terms/collectionCode\"," +
                "\"http://rs.tdwg.org/dwc/terms/catalogNumber\"," +
                "\"http://rs.tdwg.org/dwc/terms/sex\"," +
                "\"http://rs.tdwg.org/dwc/terms/lifeStage\"," +
                "\"http://rs.tdwg.org/dwc/terms/reproductiveCondition\"," +
                "\"http://rs.tdwg.org/dwc/terms/behavior\"," +
                "\"http://rs.tdwg.org/dwc/terms/establishmentMeans\"," +
                "\"http://rs.tdwg.org/dwc/terms/occurrenceRemarks\"," +
                "\"http://rs.tdwg.org/dwc/terms/individualCount\"," +
                "\"http://rs.tdwg.org/dwc/terms/preparations\"," +
                "\"http://rs.tdwg.org/dwc/terms/fieldNotes\"," +
                "\"http://rs.tdwg.org/dwc/terms/samplingProtocol\"," +
                "\"http://rs.tdwg.org/dwc/terms/samplingEffort\"," +
                "\"http://rs.tdwg.org/dwc/terms/identifiedBy\"," +
                "\"http://rs.tdwg.org/dwc/terms/dateIdentified\"," +
                "\"http://rs.tdwg.org/dwc/terms/eventDate\"," +
                "\"http://purl.org/dc/terms/modified\"," +
                "\"http://rs.tdwg.org/dwc/terms/locality\"," +
                "\"http://rs.tdwg.org/dwc/terms/decimalLatitude\"," +
                "\"http://rs.tdwg.org/dwc/terms/decimalLongitude\"," +
                "\"http://rs.tdwg.org/dwc/terms/verbatimLatitude\"," +
                "\"http://rs.tdwg.org/dwc/terms/verbatimLongitude\"," +
                "\"http://rs.tdwg.org/dwc/terms/verbatimElevation\"," +
                "\"http:/eol.org/globi/terms/physiologicalState\"," +
                "\"http:/eol.org/globi/terms/bodyPart\"";
        return header;
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
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:123");
        myStudy.collected(specimen);

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "\nglobi:occur:3,EOL:123,myStudy,,,,,,,,,,,,,,,,,,,,,,,,,,";

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:327955");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, ExportTestUtil.utcTestTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        eatWolf(specimen);
        eatWolf(specimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }

    private Specimen eatWolf(Specimen specimen) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen("Canis lupus", "EOL:328607");
        otherSpecimen.setVolumeInMilliLiter(124.0);
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
