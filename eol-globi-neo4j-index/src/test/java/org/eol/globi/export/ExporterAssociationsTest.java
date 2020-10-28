package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
public class ExporterAssociationsTest extends GraphDBTestCase {

    @Test
    public void exportWithoutHeader() throws IOException, NodeFactoryException {
        createTestData(null);
        resolveNames();

        String expected = "globi:assoc:X\tglobi:occur:X\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:X\t\t\t\t\tcontributor. pubYear. description\t\t\tglobi:ref:X\n" +
                "globi:assoc:X\tglobi:occur:X\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:X\t\t\t\t\tcontributor. pubYear. description\t\t\tglobi:ref:X\n";


        StringWriter row = new StringWriter();

        new ExporterAssociations().exportStudy(getStudySingleton(getGraphDb()), ExportUtil.AppenderWriter.of(row), false);

        ExportTestUtil.assertSameAsideFromNodeIds(row.getBuffer().toString(), expected);
    }

    private void createTestData(Double length) throws NodeFactoryException {
        Study myStudy = nodeFactory.getOrCreateStudy(new StudyImpl("myStudy", null, ExternalIdUtil.toCitation("contributor", "description", "pubYear")));
        Specimen specimen = nodeFactory.createSpecimen(myStudy, new TaxonImpl("Homo sapiens", "EOL:123"));
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        nodeFactory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        eats(specimen, "Canis lupus", "EOL:456", myStudy);
        eats(specimen, "Felis whateverus", PropertyAndValueDictionary.NO_MATCH, myStudy);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(13.0, 45.9, -60.0, null));
        specimen.caughtIn(location);
    }

    private void eats(Specimen specimen, String scientificName, String taxonExternalId, Study study) throws NodeFactoryException {
        Specimen otherSpecimen1 = nodeFactory.createSpecimen(study, new TaxonImpl(scientificName, taxonExternalId));
        otherSpecimen1.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen1);

        Specimen otherSpecimen2 = nodeFactory.createSpecimen(study, new TaxonImpl(scientificName, taxonExternalId));
        otherSpecimen2.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen2);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterAssociations());
    }

}
