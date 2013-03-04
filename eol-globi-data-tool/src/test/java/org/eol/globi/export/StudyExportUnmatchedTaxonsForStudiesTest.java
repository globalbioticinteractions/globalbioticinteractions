package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.ExternalIdTaxonEnricher;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedTaxonsForStudiesTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", "homoSapiensId");
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus other", ExternalIdTaxonEnricher.NO_MATCH);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen();
        Taxon homoSapiens2 = nodeFactory.getOrCreateTaxon("Homo sapiens2", ExternalIdTaxonEnricher.NO_MATCH);
        predatorSpecimen2.classifyAs(homoSapiens2);
        addCanisLupus(predatorSpecimen2, "canisLupusId");
        study.collected(predatorSpecimen2);

        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedTaxonsForStudies(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"name of unmatched predator taxon\",\" study title in which predator was referenced\"" +
                "\n\"Homo sapiens2\",\"my study\"\n"));
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.getOrCreateTaxon("Canis lupus", externalId);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

}