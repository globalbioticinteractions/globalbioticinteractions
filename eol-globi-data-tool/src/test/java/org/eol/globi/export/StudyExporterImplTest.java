package org.eol.globi.export;

import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StudyExporterImplTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = getExpectedHeader();
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private String getExpectedHeader() {
        String expected = "";
        expected += "\"study\",\"predator\", \"length(mm)\",\"prey\", \"latitude\", \"longitude\", \"altitude\",\"total predator stomach volume (ml)\",\"prey volume in stomach (ml)\", \"collection year\",\"collection month\",\"collection day of month\"";
        return expected;
    }

    @Test
    public void exportNoHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = "";
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        String expected = "";
        expected += getExpectedHeader();
        expected += "\n\"myStudy\",\"Homo sapiens\",123.0,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",123.0,\"Canis lupus\",123.0,345.9,-60.0,666.0,124.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));

    }

    @Test
    public void exportToCSVSpecimenEmptyStomach() throws NodeFactoryException, IOException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen();
        myStudy.collected(specimen);
        Taxon taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        specimen.classifyAs(taxon);

        StringWriter row = new StringWriter();

        new StudyExporterImpl().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "\n\"myStudy\",\"Homo sapiens\",,,,,,,,,,";


        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen();
        specimen.setStomachVolumeInMilliLiter(666.0);
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, new SimpleDateFormat("yyyy.MM.dd").parse("1992.03.30").getTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        Taxon taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        specimen.classifyAs(taxon);
        Specimen otherSpecimen = nodeFactory.createSpecimen();
        otherSpecimen.setVolumeInMilliLiter(124.0);
        Taxon wolf = nodeFactory.getOrCreateTaxon("Canis lupus");

        otherSpecimen.classifyAs(wolf);
        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }


}
