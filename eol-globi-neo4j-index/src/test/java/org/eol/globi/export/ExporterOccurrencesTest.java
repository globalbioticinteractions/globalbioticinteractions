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

import static org.hamcrest.MatcherAssert.assertThat;
public class ExporterOccurrencesTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        resolveNames();
        String expected = getExpectedHeader();
        expected += getExpectedData();

        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), true);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\\n"), expected.split("\\n"));

    }

    private String getExpectedData() {
        return "some:occur:id\tEOL:327955\tBAR\tFOO\tc678\t\tJUVENILE\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\tDIGESTATE\tBONE\n" +
                "globi:occur:X\tEOL:328607\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\t\t\n" +
                "globi:occur:X\tEOL:328607\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t1992-03-30T08:00:00Z\t\t\t12.0\t-1.0\t\t\t-60.0 m\t\t\n";
    }

    private String getExpectedHeader() {
        return "occurrenceID\ttaxonID\tinstitutionCode\tcollectionCode\tcatalogNumber\tsex\tlifeStage\treproductiveCondition\tbehavior\testablishmentMeans\toccurrenceRemarks\tindividualCount\tpreparations\tfieldNotes\tbasisOfRecord\tsamplingProtocol\tsamplingEffort\tidentifiedBy\tdateIdentified\teventDate\tmodified\tlocality\tdecimalLatitude\tdecimalLongitude\tverbatimLatitude\tverbatimLongitude\tverbatimElevation\tphysiologicalState\tbodyPart\n";
    }

    @Test
    public void exportNoHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        resolveNames();
        String expected = getExpectedData();

        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), false);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\\n"), expected.split("\\n"));
    }

    private ExporterOccurrences exportOccurrences() {
        return new ExporterOccurrences();
    }

    @Test
    public void exportToCSV() throws NodeFactoryException, IOException, ParseException {
        createTestData(123.0);
        resolveNames();
        String expected = getExpectedHeader();
        expected += getExpectedData();

        StudyNode myStudy1 = (StudyNode) nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy1, ExportUtil.AppenderWriter.of(row), true);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\\n"), expected.split("\\n"));

    }

    @Test
    public void dontExportToCSVSpecimenEmptyStomach() throws NodeFactoryException, IOException {
        StudyNode myStudy = (StudyNode) nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl("Homo sapiens", "EOL:123"));
        specimen.setBasisOfRecord(new TermImpl("test:123", "aBasisOfRecord"));
        resolveNames();

        StringWriter row = new StringWriter();

        exportOccurrences().exportStudy(myStudy, ExportUtil.AppenderWriter.of(row), true);

        String expected = "";
        expected += getExpectedHeader();
        expected += "globi:occur:X\tEOL:123\t\t\t\t\t\t\t\t\t\t\t\t\taBasisOfRecord\t\t\t\t\t\t\t\t\t\t\t\t\t\t\n";

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString().split("\\n"), expected.split("\\n"));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy(new StudyImpl("myStudy", null, null));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl("Homo sapiens", "EOL:327955"));
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        specimen.setProperty(PropertyAndValueDictionary.OCCURRENCE_ID, "some:occur:id");
        specimen.setProperty(PropertyAndValueDictionary.CATALOG_NUMBER, "c678");
        specimen.setProperty(PropertyAndValueDictionary.COLLECTION_CODE, "FOO");
        specimen.setProperty(PropertyAndValueDictionary.INSTITUTION_CODE, "BAR");
        nodeFactory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(12.0, -1.0, -60.0, null));
        specimen.caughtIn(location);
        Specimen wolf1 = eatWolf(specimen, myStudy);
        wolf1.caughtIn(location);
        Specimen wolf2 = eatWolf(specimen, myStudy);
        wolf2.caughtIn(location);
    }

    private Specimen eatWolf(Specimen specimen, Study study) throws NodeFactoryException {
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Canis lupus", "EOL:328607"));
        otherSpecimen.setVolumeInMilliLiter(124.0);
        nodeFactory.setUnixEpochProperty(otherSpecimen, ExportTestUtil.utcTestDate());
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(exportOccurrences());
    }

}
