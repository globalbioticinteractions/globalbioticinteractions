package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterOccurrenceAggregatesTest extends GraphDBTestCase {

    private String[] getExpectedData() {
        return ("globi:occur:source:X-EOL:123-ATE\tEOL:123\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:123-ATE-EOL:555\tEOL:555\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:123-ATE-EOL:666\tEOL:666\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:source:X-EOL:333-ATE\tEOL:333\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:333-ATE-EOL:555\tEOL:555\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:333-ATE-EOL:666\tEOL:666\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:source:X-EOL:888-ATE\tEOL:888\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:888-ATE-EOL:555\tEOL:555\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:888-ATE-EOL:666\tEOL:666\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:source:X-EOL:888-ATE\tEOL:888\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:888-ATE-EOL:555\tEOL:555\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "globi:occur:target:X-EOL:888-ATE-EOL:666\tEOL:666\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n").split("\n");
    }

    @Test
    public void exportNoMatchName() throws NodeFactoryException, IOException {
        StudyNode myStudy = (StudyNode) nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        nodeFactory.createSpecimen(myStudy, new TaxonImpl(PropertyAndValueDictionary.NO_MATCH, "some externalid"));
        resolveNames();

        StringWriter row = new StringWriter();
        new ExporterOccurrenceAggregates().exportDistinct(myStudy, ExportUtil.AppenderWriter.of(row));
        assertThat(row.toString(), equalTo(""));
    }

    @Test
    public void exportToCSVNoHeader() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        resolveNames();

        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterOccurrenceAggregates().exportDistinct(myStudy1, ExportUtil.AppenderWriter.of(row));


        String actualData = row.getBuffer().toString();
        String[] expectedData = getExpectedData();
        ExportTestUtil.assertSameAsideFromNodeIds(actualData.split("\n"), expectedData);

    }


    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, nodeFactory.createStudy(new StudyImpl("yourStudy", null, null)), "Homo erectus", "EOL:888");
        specimenEatCatAndDog(length, nodeFactory.createStudy(new StudyImpl("yourStudy2", null, null)), "Homo erectus", "EOL:888");
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

        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(12.0, -45.9, -60.0, null));
        specimen.caughtIn(location);
    }

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl(scientificName, externalId));
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        return specimen;
    }

    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId, Study study) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(scientificName, externalId));
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
