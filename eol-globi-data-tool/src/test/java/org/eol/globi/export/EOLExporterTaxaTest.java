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

public class EOLExporterTaxaTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);

        String expected =
                "\nEOL:123,Canis lupus,,,,,,,,,,,,,\nEOL:45634,Homo sapiens,,,,,,,,,,,,,";


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new EOLExporterTaxa().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:45634");
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
        Specimen otherSpecimen = nodeFactory.createSpecimen("Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);

        otherSpecimen = nodeFactory.createSpecimen("Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        EOLExporterTaxa exporter = new EOLExporterTaxa();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}