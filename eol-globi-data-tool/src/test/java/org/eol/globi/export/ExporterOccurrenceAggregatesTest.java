package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.LifeStage;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterOccurrenceAggregatesTest extends GraphDBTestCase {

    private String getExpectedData() {
        return "\n" +
                "globi:occur:1-23-ATE,EOL:123,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-23-ATE-5,EOL:555,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-23-ATE-8,EOL:666,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-2-ATE,EOL:333,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-2-ATE-5,EOL:555,,,,,,,,,,,,,,,,,,,,,,myStudy\n" +
                "globi:occur:1-2-ATE-8,EOL:666,,,,,,,,,,,,,,,,,,,,,,myStudy";
    }

    private ExporterOccurrencesBase exportOccurrences() {
        return new ExporterOccurrenceAggregates();
    }

    @Test
    public void exportToCSVNoHeader() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, row, false);

        String expectedData = getExpectedData();

        int linesPerHeader = 1;
        int numberOfExpectedDistinctSourceTargetInteractions = 6;
        String actualData = row.getBuffer().toString();
        assertThat(actualData, equalTo(expectedData));
        assertThat(actualData.split("\n").length, Is.is(linesPerHeader + numberOfExpectedDistinctSourceTargetInteractions));

    }


    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, myStudy, "Blo blaaus", PropertyAndValueDictionary.NO_MATCH);
    }

    private void specimenEatCatAndDog(Double length, Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        Specimen specimen = collectSpecimen(myStudy, scientificName, externalId);
        eatPrey(specimen, "Canis lupus", "EOL:555");
        eatPrey(specimen, "Felis domesticus", "EOL:666");
        eatPrey(specimen, "Blah blahuuuu", PropertyAndValueDictionary.NO_MATCH);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(scientificName, externalId);
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
        return specimen;
    }

    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(scientificName, externalId);
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
