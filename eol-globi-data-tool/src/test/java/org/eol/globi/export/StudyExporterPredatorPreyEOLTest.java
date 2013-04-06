package org.eol.globi.export;

import org.junit.Test;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExporterPredatorPreyEOLTest extends GraphDBTestCase {

    @Test
    public void export() throws NodeFactoryException, IOException {
        String predatorExternalId = "sapiensId";
        String preyExternalId = "lupusId";
        Study study = createStudy(predatorExternalId, preyExternalId);

        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);

        assertThat(writer.toString(), is("\"sapiensId\",\"ATE\",\"lupusId\"\n"));
    }

    private Study createStudy(String predatorExternalId, String preyExternalId) throws NodeFactoryException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens", predatorExternalId);
        addCanisLupus(predatorSpecimen, preyExternalId);
        study.collected(predatorSpecimen);
        return study;
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus", externalId);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

    @Test
    public void exportPreyNoExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy("some external id", null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void exportNoPredatorExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, "some external id");
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void exportNoPredatorExternalIdNoPreyExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }


    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens", "homoSapiensId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus other", "canisLupusId2");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen("Homo sapiens2", "homoSapiensId2");
        addCanisLupus(predatorSpecimen2, "canisLupusId");
        study.collected(predatorSpecimen2);

        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"homoSapiensId\",\"ATE\",\"canisLupusId\"\n\"homoSapiensId2\",\"ATE\",\"canisLupusId\"\n\"homoSapiensId\",\"ATE\",\"canisLupusId2\"\n"));
    }
}
