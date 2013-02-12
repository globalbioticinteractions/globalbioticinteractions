package org.trophic.graph.export;

import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.InteractType;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

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
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", predatorExternalId);
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, preyExternalId);
        study.collected(predatorSpecimen);
        return study;
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus", externalId);
        preySpecimen.classifyAs(canisLupus);
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
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", "homoSapiensId");
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus other", "canisLupusId2");
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen();
        Taxon homoSapiens2 = nodeFactory.getOrCreateTaxon("Homo sapiens2", "homoSapiensId2");
        predatorSpecimen2.classifyAs(homoSapiens2);
        addCanisLupus(predatorSpecimen2, "canisLupusId");
        study.collected(predatorSpecimen2);

        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"homoSapiensId\",\"ATE\",\"canisLupusId\"\n\"homoSapiensId2\",\"ATE\",\"canisLupusId\"\n\"homoSapiensId\",\"ATE\",\"canisLupusId2\"\n"));
    }
}
