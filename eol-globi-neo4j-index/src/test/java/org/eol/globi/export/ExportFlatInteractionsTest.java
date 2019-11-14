package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
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

import static org.hamcrest.Matchers.containsString;
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
        new ExportFlatInteractions(new ExportUtil.TsvValueJoiner(), "interactions.tsv.gz").export(getGraphDb(), ExportUtil.AppenderWriter.of(writer));
        final String actualExport = writer.toString();
        final String[] actualExportLines = actualExport.split("\\n");
        final String header = "sourceTaxonId\tsourceTaxonIds\tsourceTaxonName\tsourceTaxonRank\tsourceTaxonPathNames\tsourceTaxonPathIds\tsourceTaxonPathRankNames\tsourceTaxonSpeciesName\tsourceTaxonSpeciesId\tsourceTaxonGenusName\tsourceTaxonGenusId\tsourceTaxonFamilyName\tsourceTaxonFamilyId\tsourceTaxonOrderName\tsourceTaxonOrderId\tsourceTaxonClassName\tsourceTaxonClassId\tsourceTaxonPhylumName\tsourceTaxonPhylumId\tsourceTaxonKingdomName\tsourceTaxonKingdomId\tsourceId\tsourceOccurrenceId\tsourceCatalogNumber\tsourceBasisOfRecordId\tsourceBasisOfRecordName\tsourceLifeStageId\tsourceLifeStageName\tsourceBodyPartId\tsourceBodyPartName\tsourcePhysiologicalStateId\tsourcePhysiologicalStateName\tinteractionTypeName\tinteractionTypeId\ttargetTaxonId\ttargetTaxonIds\ttargetTaxonName\ttargetTaxonRank\ttargetTaxonPathNames\ttargetTaxonPathIds\ttargetTaxonPathRankNames\ttargetTaxonSpeciesName\ttargetTaxonSpeciesId\ttargetTaxonGenusName\ttargetTaxonGenusId\ttargetTaxonFamilyName\ttargetTaxonFamilyId\ttargetTaxonOrderName\ttargetTaxonOrderId\ttargetTaxonClassName\ttargetTaxonClassId\ttargetTaxonPhylumName\ttargetTaxonPhylumId\ttargetTaxonKingdomName\ttargetTaxonKingdomId\ttargetId\ttargetOccurrenceId\ttargetCatalogNumber\ttargetBasisOfRecordId\ttargetBasisOfRecordName\ttargetLifeStageId\ttargetLifeStageName\ttargetBodyPartId\ttargetBodyPartName\ttargetPhysiologicalStateId\ttargetPhysiologicalStateName\tdecimalLatitude\tdecimalLongitude\tlocalityId\tlocalityName\teventDateUnixEpoch\targumentTypeId\treferenceCitation\treferenceDoi\treferenceUrl\tsourceCitation\tsourceNamespace\tsourceArchiveURI\tsourceDOI\tsourceLastSeenAtUnixEpoch";
        final String first = "\nEOL:333\t\tHomo sapiens\t\tpathElem1 | pathElem 2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tGLOBI:JUVENILE\tJUVENILE\tGLOBI:BONE\tBONE\tGLOBI:DIGESTATE\tDIGESTATE\teats\thttp://purl.obolibrary.org/obo/RO_0002470\tEOL:555\t\tCanis lupus\t\tpreyPathElem1 | preyPathElem2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t12.0\t-45.9\tsome:localeid\tsome locale\t701942400000\thttps://en.wiktionary.org/wiki/support\t\t\t\t<http://example.com>\tsome/namespace\thttp://example.com\t\t";
        final String firstRefute = "\nEOL:333\t\tHomo sapiens\t\tpathElem1 | pathElem 2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tGLOBI:JUVENILE\tJUVENILE\tGLOBI:BONE\tBONE\tGLOBI:DIGESTATE\tDIGESTATE\teats\thttp://purl.obolibrary.org/obo/RO_0002470\tEOL:555\t\tCanis lupus\t\tpreyPathElem1 | preyPathElem2\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t12.0\t-45.9\tsome:localeid\tsome locale\t701942400000\thttps://en.wiktionary.org/wiki/refute\t\t\t\t<http://example.com>\tsome/namespace\thttp://example.com\t\t";
        assertThat(actualExportLines.length, is(22));
        assertThat(actualExportLines[0], is(header));
        assertThat(actualExport, containsString(first));
        assertThat(actualExport, containsString(firstRefute));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy(new StudyImpl("myStudy", null, null, null));
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333", RelTypes.REFUTES);
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy", null, null, null)), "Homo erectus", "EOL:888", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy2", null, null, null)), "Homo erectus", "EOL:888", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Blo blaaus", PropertyAndValueDictionary.NO_MATCH, RelTypes.COLLECTED, RelTypes.SUPPORTS);
    }


    private void specimenEatCatAndDog(Double length, Study myStudy, String scientificName, String externalId, RelTypes... relTypes) throws NodeFactoryException {
        Specimen specimen = collectSpecimen(myStudy, scientificName, externalId, relTypes);
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

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId, RelTypes... relTypes) throws NodeFactoryException {
        final TaxonImpl taxon = new TaxonImpl(scientificName, externalId);
        taxon.setPath("pathElem1 | pathElem 2");
        Specimen specimen = factory.createSpecimen(myStudy, taxon, relTypes);
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
