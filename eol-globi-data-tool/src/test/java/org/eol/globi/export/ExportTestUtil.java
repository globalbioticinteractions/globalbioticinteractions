package org.eol.globi.export;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
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
    public static StudyNode createTestData(NodeFactory factory) throws NodeFactoryException, ParseException {
        return createTestData(123.4, factory);
    }

    public static StudyNode createTestData(Double length, NodeFactory factory) throws NodeFactoryException, ParseException {
        StudyNode myStudy = factory.createStudy("myStudy");
        SpecimenNode specimen1 = factory.createSpecimen(myStudy, "Homo sapiens", "EOL:45634");
        specimen1.setStomachVolumeInMilliLiter(666.0);
        specimen1.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen1.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen1.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        factory.setUnixEpochProperty(specimen1, ExportTestUtil.utcTestDate());
        final SpecimenNode specimen2 = factory.createSpecimen(myStudy, "Canis lupus", "EOL:123");
        specimen2.setVolumeInMilliLiter(124.0);

        specimen1.ate(specimen2);

        final SpecimenNode specimen3 = factory.createSpecimen(myStudy, "Canis lupus", "EOL:123");
        specimen3.setVolumeInMilliLiter(18.0);
        specimen1.ate(specimen3);
        if (null != length) {
            specimen1.setLengthInMm(length);
        }

        Location location = factory.getOrCreateLocation(88.0, -120.0, -60.0);
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
        return new TaxonIndexImpl(taxonEnricher, new CorrectionService() {

            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, graphDb);
    }

    public static void assertFileInMeta(ExporterBase exporter) throws IOException {
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.tsv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.tsv" + exporter.getMetaTableSuffix()));
    }
}
