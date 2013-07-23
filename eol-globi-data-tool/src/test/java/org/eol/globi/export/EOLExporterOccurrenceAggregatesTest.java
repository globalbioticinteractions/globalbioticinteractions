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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class EOLExporterOccurrenceAggregatesTest extends GraphDBTestCase {

    private String getExpectedData() {
        return "\nglobi:occur:1-2-ATE-5,EOL:327955,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-2-ATE,EOL:328607,,,,,,,,,,,,,,,,,,,,,,myStudy";
    }

    private EOLExporterOccurrencesBase exportOccurrences() {
        return new EOLExporterOccurrenceAggregates();
    }

    @Test
    public void exportToCSVNoHeader() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        String expected = "";
        expected += getExpectedData();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, false);

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

}
