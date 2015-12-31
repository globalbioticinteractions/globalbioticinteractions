package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterOccurrenceAggregatesTest extends GraphDBTestCase {

    private String getExpectedData() {
        return "\n" +
                "globi:occur:source:1-EOL:123-ATE,EOL:123,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:1-EOL:123-ATE-EOL:555,EOL:555,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:1-EOL:123-ATE-EOL:666,EOL:666,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:source:1-EOL:333-ATE,EOL:333,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:1-EOL:333-ATE-EOL:555,EOL:555,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:1-EOL:333-ATE-EOL:666,EOL:666,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:source:36-EOL:888-ATE,EOL:888,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:36-EOL:888-ATE-EOL:555,EOL:555,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:36-EOL:888-ATE-EOL:666,EOL:666,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:source:45-EOL:888-ATE,EOL:888,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:45-EOL:888-ATE-EOL:555,EOL:555,,,,,,,,,,,,,,,,,,,,,,,,,,,\n" +
                "globi:occur:target:45-EOL:888-ATE-EOL:666,EOL:666,,,,,,,,,,,,,,,,,,,,,,,,,,,";
    }

    @Test
    public void exportNoMatchName() throws NodeFactoryException, IOException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        nodeFactory.createSpecimen(myStudy, PropertyAndValueDictionary.NO_MATCH, "some externalid");
        resolveNames();

        StringWriter row = new StringWriter();
        new ExporterOccurrenceAggregates().exportDistinct(myStudy, row);
        assertThat(row.toString(), equalTo(""));
    }

    @Test
    public void exportToCSVNoHeader() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        resolveNames();

        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterOccurrenceAggregates().exportDistinct(myStudy1, row);

        String expectedData = getExpectedData();

        int linesPerHeader = 1;
        int numberOfExpectedDistinctSourceTargetInteractions = 12;
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
        specimenEatCatAndDog(length, nodeFactory.createStudy("yourStudy"), "Homo erectus", "EOL:888");
        specimenEatCatAndDog(length, nodeFactory.createStudy("yourStudy2"), "Homo erectus", "EOL:888");
        specimenEatCatAndDog(length, myStudy, "Blo blaaus", PropertyAndValueDictionary.NO_MATCH);
    }

    private void specimenEatCatAndDog(Double length, Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        Specimen specimen = collectSpecimen(myStudy, scientificName, externalId);
        eatPrey(specimen, "Canis lupus", "EOL:555", myStudy);
        eatPrey(specimen, "Felis domesticus", "EOL:666", myStudy);
        eatPrey(specimen, "Blah blahuuuu", PropertyAndValueDictionary.NO_MATCH, myStudy);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(12.0, -45.9, -60.0);
        specimen.caughtIn(location);
    }

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(myStudy, scientificName, externalId);
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        return specimen;
    }

    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId, Study study) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, scientificName, externalId);
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
