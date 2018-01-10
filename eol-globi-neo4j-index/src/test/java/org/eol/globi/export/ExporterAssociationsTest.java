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
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExporterAssociationsTest extends GraphDBTestCase {

    @Test
    public void exportWithoutHeader() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);
        resolveNames();

        String expected = "\nglobi:assoc:4\tglobi:occur:2\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:4\t\t\t\t\tdata source description\t\t\tglobi:ref:1" +
                "\nglobi:assoc:6\tglobi:occur:2\thttp://purl.obolibrary.org/obo/RO_0002470\tglobi:occur:4\t\t\t\t\tdata source description\t\t\tglobi:ref:1";


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new ExporterAssociations().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.getOrCreateStudy(new StudyImpl("myStudy", "data\tsource description", null, ExternalIdUtil.toCitation("contributor", "description", "pubYear")));
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
        Specimen otherSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(scientificName, taxonExternalId));
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);
        specimen.ate(otherSpecimen);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        ExportTestUtil.assertFileInMeta(new ExporterAssociations());
    }

}
