package org.eol.globi.export;

import org.eol.globi.data.LifeStage;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InteractionsExporterTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = getExpectedHeader();
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new InteractionsExporter().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private String getExpectedHeader() {
        String expected = "";
        expected += "\"study\",\"sourceTaxonName\",\"sourceLifeStage\",\"sourceTaxonId\",\"interactType\",\"targetTaxonName\",\"targetLifeStage\",\"targetTaxonId\",\"latitude\",\"longitude\",\"altitude\",\"collection year\",\"collection month\",\"collection day of month\"";
        return expected;
    }

    @Test
    public void exportNoHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        String expected = "";
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new InteractionsExporter().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        String expected = "";
        expected += getExpectedHeader();
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";
        expected += "\n\"myStudy\",\"Homo sapiens\",\"JUVENILE\",,\"ATE\",\"Canis lupus\",,,123.0,345.9,-60.0,1992,3,30";

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new InteractionsExporter().exportStudy(myStudy1, row, true);

        assertThat(row.getBuffer().toString(), equalTo(expected));

    }

    @Test
    public void dontExportToCSVSpecimenEmptyStomach() throws NodeFactoryException, IOException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens");
        myStudy.collected(specimen);

        StringWriter row = new StringWriter();

        new InteractionsExporter().exportStudy(myStudy, row, true);

        String expected = "";
        expected += getExpectedHeader();

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(LifeStage.JUVENILE);
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, new SimpleDateFormat("yyyy.MM.dd").parse("1992.03.30").getTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        Specimen otherSpecimen = nodeFactory.createSpecimen("Canis lupus");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        InteractionsExporter exporter = new InteractionsExporter();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}
