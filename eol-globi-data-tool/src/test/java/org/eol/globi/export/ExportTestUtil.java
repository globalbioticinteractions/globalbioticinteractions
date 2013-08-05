package org.eol.globi.export;

import org.eol.globi.data.LifeStage;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import javax.xml.bind.DatatypeConverter;
import java.text.ParseException;
import java.util.Calendar;

public class ExportTestUtil {
    public static Study createTestData( NodeFactory factory) throws NodeFactoryException, ParseException {
        return createTestData(123.4, factory);
    }

    public static Study createTestData(Double length, NodeFactory factory) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy("myStudy");
        Specimen specimen = factory.createSpecimen("Homo sapiens", "EOL:45634");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(LifeStage.JUVENILE);
        specimen.setPhysiologicalState(PhysiologicalState.DIGESTATE);
        specimen.setBodyPart(BodyPart.BONE);
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, utcTestTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        Specimen otherSpecimen = factory.createSpecimen("Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);

        otherSpecimen = factory.createSpecimen("Canis lupus", "EOL:123");
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = factory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
        return myStudy;
    }

    public static long utcTestTime() {
        Calendar calendar = DatatypeConverter.parseDateTime("1992-03-30T08:00:00Z");
        return calendar.getTime().getTime();
    }
}
