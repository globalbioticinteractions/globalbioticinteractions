package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportFlatInteractionsTest extends GraphDBTestCase {


    @Test
    public void importExportCompareResults() throws NodeFactoryException, IOException, ParseException {
        createTestData(12.0);
        resolveNames();

        final StringWriter writer = new StringWriter();
        new ExportFlatInteractions().export(getGraphDb(), writer);
        final String actualExport = writer.toString();
        final String[] actualExportLines = actualExport.split("\\n");
        assertThat(actualExportLines.length, is(22));
        final String header = "sourceTaxonId\tsourceTaxonName\tsourceTaxonRank\tsourceTaxonPathNames\tsourceTaxonPathIds\tsourceTaxonPathRankNames\tinteractionTypeName\tinteractionTypeId\ttargetTaxonId\ttargetTaxonName\ttargetTaxonRank\ttargetTaxonPathNames\ttargetTaxonPathIds\ttargetTaxonPathRankNames\tdecimalLatitude\tdecimalLongitude\tlocality\teventDateUnixEpoch\treferenceCitation\treferenceDoi\treferenceUrl\tsourceCitation";
        final String first = "EOL:333\tHomo sapiens\t\tpathElem1 | pathElem 2\t\t\teats\thttp://purl.obolibrary.org/obo/RO_0002470\tEOL:555\tCanis lupus\t\tpreyPathElem1 | preyPathElem2\t\t\t12.0\t-45.9\t\t701942400000\t\t\t\t";
        assertThat(actualExportLines[0], is(header));
        assertThat(actualExportLines[1], is(first));
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
        final TaxonImpl taxon = new TaxonImpl(scientificName, externalId);
        taxon.setPath("pathElem1 | pathElem 2");
        Specimen specimen = nodeFactory.createSpecimen(myStudy, taxon);
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        return specimen;
    }


    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId, Study study) throws NodeFactoryException {
        final TaxonImpl preyTaxon = new TaxonImpl(scientificName, externalId);
        preyTaxon.setPath("preyPathElem1 | preyPathElem2");
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, preyTaxon);
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
