package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.LifeStage;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLExporterOccurrencesTest extends GraphDBTestCase {

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
        return "\nglobi:occur:3,EOL:327955,,JUVENILE,DIGESTATE,BONE,,,,,,,,,,123.0,345.9,,-60.0,,,,1992-03-30T08:00:00Z,myStudy" +
                "\nglobi:occur:6,EOL:328607,,,,,,,,,,,,,,123.0,345.9,,-60.0,,,,1992-03-30T08:00:00Z,myStudy" +
                "\nglobi:occur:8,EOL:328607,,,,,,,,,,,,,,123.0,345.9,,-60.0,,,,1992-03-30T08:00:00Z,myStudy";
    }

    private String getExpectedHeader() {
        String header = "\"occurrenceID\",\"taxonID\",\"sex\",\"lifeStage\",\"physiologicalState\",\"bodyPart\",\"reproductiveCondition\",\"behavior\",\"establishmentMeans\",\"occurrenceRemarks\",\"individualCount\",\"preparations\",\"fieldNotes\",\"samplingProtocol\",\"samplingEffort\",\"decimalLatitude\",\"decimalLongitude\",\"depth\",\"altitude\",\"locality\",\"identifiedBy\",\"dateIdentified\",\"eventDate\",\"eventID\"" +
                "\n\"http://rs.tdwg.org/dwc/terms/occurrenceID\",\"http://rs.tdwg.org/dwc/terms/taxonID\",\"http://rs.tdwg.org/dwc/terms/sex\",\"http://rs.tdwg.org/dwc/terms/lifeStage\",\"http:/eol.org/globi/terms/physiologicalState\",\"http:/eol.org/globi/terms/bodyPart\",\"http://rs.tdwg.org/dwc/terms/reproductiveCondition\",\"http://rs.tdwg.org/dwc/terms/behavior\",\"http://rs.tdwg.org/dwc/terms/establishmentMeans\",\"http://rs.tdwg.org/dwc/terms/occurrenceRemarks\",\"http://rs.tdwg.org/dwc/terms/individualCount\",\"http://rs.tdwg.org/dwc/terms/preparations\",\"http://rs.tdwg.org/dwc/terms/fieldNotes\",\"http://rs.tdwg.org/dwc/terms/samplingProtocol\",\"http://rs.tdwg.org/dwc/terms/samplingEffort\",\"http://rs.tdwg.org/dwc/terms/decimalLatitude\",\"http://rs.tdwg.org/dwc/terms/decimalLongitude\",\"http://eol.org/globi/depth\",\"http://eol.org/globi/altitude\",\"http://rs.tdwg.org/dwc/terms/locality\",\"http://rs.tdwg.org/dwc/terms/identifiedBy\",\"http://rs.tdwg.org/dwc/terms/dateIdentified\",\"http://rs.tdwg.org/dwc/terms/eventDate\",\"http://rs.tdwg.org/dwc/terms/eventID\"";
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

    private EOLExporterOccurrences exportOccurrences() {
        return new EOLExporterOccurrences();
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
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens");
        myStudy.collected(specimen);

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "\nglobi:occur:3,,,,,,,,,,,,,,,,,,,,,,,myStudy";

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:327955");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(LifeStage.JUVENILE);
        specimen.setPhysiologicalState(PhysiologicalState.DIGESTATE);
        specimen.setBodyPart(BodyPart.BONE);
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, getUTCTestTime());
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
        EOLExporterOccurrences exporter = exportOccurrences();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}
