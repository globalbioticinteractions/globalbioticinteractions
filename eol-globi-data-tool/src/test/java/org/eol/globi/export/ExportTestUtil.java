package org.eol.globi.export;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.taxon.CorrectionService;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.xml.bind.DatatypeConverter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class ExportTestUtil {
    public static Study createTestData( NodeFactory factory) throws NodeFactoryException, ParseException {
        return createTestData(123.4, factory);
    }

    public static Study createTestData(Double length, NodeFactory factory) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy("myStudy");
        Specimen specimen = factory.createSpecimen(myStudy, "Homo sapiens", "EOL:45634");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new Term("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new Term("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new Term("GLOBI:BONE", "BONE"));
        factory.setUnixEpochProperty(specimen, ExportTestUtil.utcTestDate());
        Specimen otherSpecimen = factory.createSpecimen(myStudy, "Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);

        otherSpecimen = factory.createSpecimen(myStudy, "Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = factory.getOrCreateLocation(88.0, -120.0, -60.0);
        specimen.caughtIn(location);
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
}
