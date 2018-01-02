package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.DatasetImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class ExportFlatInteractionsTest extends GraphDBTestCase {

    private NodeFactoryWithDatasetContext factory;

    @Before
    public void init() throws IOException {
        super.startGraphDb();
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        factory = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
    }


    @Test
    public void importExportCompareResults() throws NodeFactoryException, IOException, ParseException {
        createTestData(12.0);
        resolveNames();

        final StringWriter writer = new StringWriter();
        new ExportFlatInteractions().export(getGraphDb(), writer);
        final String actualExport = writer.toString();
        final String[] actualExportLines = actualExport.split("\\n");
        assertThat(actualExportLines.length, is(22));
        final String header = "sourceTaxonId\tsourceTaxonIds\tsourceTaxonName\tsourceTaxonRank\tsourceTaxonPathNames\tsourceTaxonPathIds\tsourceTaxonPathRankNames\tsourceId\tsourceOccurrenceId\tsourceCatalogNumber\tsourceBasisOfRecordId\tsourceBasisOfRecordName\tsourceLifeStageId\tsourceLifeStageName\tsourceBodyPartId\tsourceBodyPartName\tsourcePhysiologicalStateId\tsourcePhysiologicalStateName\tinteractionTypeName\tinteractionTypeId\ttargetTaxonId\ttargetTaxonIds\ttargetTaxonName\ttargetTaxonRank\ttargetTaxonPathNames\ttargetTaxonPathIds\ttargetTaxonPathRankNames\ttargetId\ttargetOccurrenceId\ttargetCatalogNumber\ttargetBasisOfRecordId\ttargetBasisOfRecordName\ttargetLifeStageId\ttargetLifeStageName\ttargetBodyPartId\ttargetBodyPartName\ttargetPhysiologicalStateId\ttargetPhysiologicalStateName\tdecimalLatitude\tdecimalLongitude\tlocalityId\tlocalityName\teventDateUnixEpoch\treferenceCitation\treferenceDoi\treferenceUrl\tsourceCitation\tsourceNamespace\tsourceArchiveURI\tsourceDOI\tsourceLastSeenAtUnixEpoch";
        final String first = "EOL:333\t\tHomo sapiens\t\tpathElem1 | pathElem 2\t\t\t\t\t\t\t\tGLOBI:JUVENILE\tJUVENILE\tGLOBI:BONE\tBONE\tGLOBI:DIGESTATE\tDIGESTATE\teats\thttp://purl.obolibrary.org/obo/RO_0002470\tEOL:555\t\tCanis lupus\t\tpreyPathElem1 | preyPathElem2\t\t\t\t\t\t\t\t\t\t\t\t\t\t12.0\t-45.9\tsome:localeid\tsome locale\t701942400000\t\t\t\tno citation\tsome/namespace\thttp://example.com\t\t";

        assertThat(actualExportLines[0], is(header));
        assertThat(actualExportLines[1], startsWith(first));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy(new StudyImpl("myStudy", null, null, null));
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123");
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy", null, null, null)), "Homo erectus", "EOL:888");
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy2", null, null, null)), "Homo erectus", "EOL:888");
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

        LocationImpl location1 = new LocationImpl(12.0, -45.9, -60.0, null);
        location1.setLocality("some locale");
        location1.setLocalityId("some:localeid");
        Location location = factory.getOrCreateLocation(location1);
        specimen.caughtIn(location);
    }

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId) throws NodeFactoryException {
        final TaxonImpl taxon = new TaxonImpl(scientificName, externalId);
        taxon.setPath("pathElem1 | pathElem 2");
        Specimen specimen = factory.createSpecimen(myStudy, taxon);
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        factory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        return specimen;
    }


    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId, Study study) throws NodeFactoryException {
        final TaxonImpl preyTaxon = new TaxonImpl(scientificName, externalId);
        preyTaxon.setPath("preyPathElem1 | preyPathElem2");
        Specimen otherSpecimen = factory.createSpecimen(study, preyTaxon);
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
