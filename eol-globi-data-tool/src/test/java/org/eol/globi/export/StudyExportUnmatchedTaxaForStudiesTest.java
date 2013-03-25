package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.NoMatchService;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedTaxaForStudiesTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", "homoSapiensId", null);
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus other", NoMatchService.NO_MATCH, null);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Taxon homoSapiens2 = nodeFactory.getOrCreateTaxon("Homo sapiens2", NoMatchService.NO_MATCH, null);
        addSpecimen(study, homoSapiens2);
        homoSapiens2 = nodeFactory.getOrCreateTaxon("Homo sapiens2");
        addSpecimen(study, homoSapiens2);

        Study study2 = nodeFactory.createStudy("my study2");
        addSpecimen(study2, homoSapiens2);

        Taxon homoSapiens3 = nodeFactory.getOrCreateTaxon("Homo sapiens3", NoMatchService.NO_MATCH, null);
        addSpecimen(study, homoSapiens3);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedTaxaForStudies(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"name of unmatched source taxon\",\"study\"" +
                "\n\"Homo sapiens2\",\"my study\"\n" +
                "\"Homo sapiens3\",\"my study\"\n" +
                "\"Homo sapiens2\",\"my study2\"\n"));
    }

    private void addSpecimen(Study study, Taxon homoSapiens2) throws NodeFactoryException {
        Specimen predatorSpecimen2 = nodeFactory.createSpecimen();
        predatorSpecimen2.classifyAs(homoSapiens2);
        addCanisLupus(predatorSpecimen2, "canisLupusId");
        study.collected(predatorSpecimen2);
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus", externalId, null);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

}