package org.eol.globi.export;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.util.DateUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
public class ExportTestUtil {
    public static StudyNode createTestData(NodeFactory factory) throws NodeFactoryException, ParseException {
        return createTestData(123.4, factory);
    }

    public static StudyNode createTestData(Double length, NodeFactory factory) throws NodeFactoryException, ParseException {
        StudyNode myStudy = (StudyNode) factory.createStudy(new StudyImpl("myStudy", null, null));
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
        return DateUtil.parseDateUTC("1992-03-30T08:00:00Z").toDate();
    }

    public static TaxonIndex taxonIndexWithEnricher(PropertyEnricher taxonEnricher, GraphDatabaseService graphDb) {
        return new NonResolvingTaxonIndexNeo4j2(graphDb);
    }

    public static void assertFileInMeta(ExporterBase exporter) throws IOException {
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.tsv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.tsv" + exporter.getMetaTableSuffix()));
    }

    static void assertSameAsideFromNodeIds(String actual, String expected) {
        assertSameAsideFromNodeIds(actual.split("\\n"), expected.split("\\n"));
    }

    static void assertSameAsideFromNodeIds(String[] actualLines, String[] expectedLines) {
        Stream<String> actual = Stream.of(actualLines)
                .map(line -> line.replaceAll("([a-z]):\\d+", "$1:X"));
        List<String> collect = actual.collect(Collectors.toList());
        assertThat(collect, containsInAnyOrder(expectedLines));
        assertThat(collect.size(), is(expectedLines.length));
    }
}
