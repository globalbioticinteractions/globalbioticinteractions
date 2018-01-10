package org.eol.globi.export;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExportTestUtil {
    public static Study createTestData(NodeFactory factory) throws NodeFactoryException, ParseException {
        return createTestData(123.4, factory);
    }

    public static Study createTestData(Double length, NodeFactory factory) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy(new StudyImpl("myStudy", null, null, null));
        Specimen specimen1 = factory.createSpecimen(myStudy, new TaxonImpl("Homo sapiens", "EOL:45634"));
        specimen1.setStomachVolumeInMilliLiter(666.0);
        specimen1.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen1.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen1.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        factory.setUnixEpochProperty(specimen1, ExportTestUtil.utcTestDate());
        final Specimen specimen2 = factory.createSpecimen(myStudy, new TaxonImpl("Canis lupus", "EOL:123"));
        specimen2.setVolumeInMilliLiter(124.0);

        specimen1.ate(specimen2);

        final Specimen specimen3 = factory.createSpecimen(myStudy, new TaxonImpl("Canis lupus", "EOL:123"));
        specimen3.setVolumeInMilliLiter(18.0);
        specimen1.ate(specimen3);
        if (null != length) {
            specimen1.setLengthInMm(length);
        }

        Location location = factory.getOrCreateLocation(new LocationImpl(88.0, -120.0, -60.0, null));
        specimen1.caughtIn(location);
        return myStudy;
    }

    public static long utcTestTime() {
        Date time = utcTestDate();
        return time.getTime();
    }

    protected static Date utcTestDate() {
        Calendar calendar = DatatypeConverter.parseDateTime("1992-03-30T08:00:00Z");
        return calendar.getTime();
    }

    public static TaxonIndex taxonIndexWithEnricher(PropertyEnricher taxonEnricher, GraphDatabaseService graphDb) {
        return new NonResolvingTaxonIndex(graphDb);
    }

    public static void assertFileInMeta(ExporterBase exporter) throws IOException {
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.tsv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.tsv" + exporter.getMetaTableSuffix()));
    }
}
