package org.eol.globi.export;

import org.junit.Test;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExporterPredatorPreyTest extends GraphDBTestCase {

    @Test
    public void export() throws NodeFactoryException, IOException {
        String predatorExternalId = "some externalId";
        String preyExternalId = "some external id";
        Study study = createStudy(predatorExternalId, preyExternalId);

        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPrey(getGraphDb()).exportStudy(study, writer, true);

        assertThat(writer.toString(), is("\"Homo sapiens\",\"Canis lupus\"\n"));
    }

    private Study createStudy(String predatorExternalId, String preyExternalId) throws NodeFactoryException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", predatorExternalId, null);
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, preyExternalId);
        study.collected(predatorSpecimen);
        return study;
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus", externalId, null);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.ate(preySpecimen);
    }

    @Test
    public void exportPreyNoExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy("some external id", null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPrey(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void exportNoPredatorExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, "some external id");
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPrey(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

@Test
    public void exportNoPredatorExternalIdNoPreyExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPrey(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }


    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", "some external id", null);
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, "some external id");
        addCanisLupus(predatorSpecimen, "some external id");
        study.collected(predatorSpecimen);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPrey(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"Homo sapiens\",\"Canis lupus\"\n\"Homo sapiens\",\"Canis lupus\"\n"));
    }
}
