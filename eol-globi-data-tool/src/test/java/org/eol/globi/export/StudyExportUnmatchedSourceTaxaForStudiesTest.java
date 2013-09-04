package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedSourceTaxaForStudiesTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens", "homoSapiensId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus other", PropertyAndValueDictionary.NO_MATCH);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen23 = nodeFactory.createSpecimen("Homo sapiens2 (bla)", PropertyAndValueDictionary.NO_MATCH);
        addCanisLupus(predatorSpecimen23, "canisLupusId");
        study.collected(predatorSpecimen23);
        Specimen predatorSpecimen22 = nodeFactory.createSpecimen("Homo sapiens2 (bla)");
        addCanisLupus(predatorSpecimen22, "canisLupusId");
        study.collected(predatorSpecimen22);

        Study study2 = nodeFactory.createStudy("my study2");
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen("Homo sapiens2 (bla)");
        addCanisLupus(predatorSpecimen21, "canisLupusId");
        study2.collected(predatorSpecimen21);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen("Homo sapiens3 (blah)", PropertyAndValueDictionary.NO_MATCH);
        addCanisLupus(predatorSpecimen2, "canisLupusId");
        study.collected(predatorSpecimen2);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"original source taxon name\",\"unmatched normalized source taxon name\",\"study\"\n" +
                "\"Homo sapiens2 (bla)\",\"Homo sapiens2\",\"my study\"\n" +
                "\"Homo sapiens3 (blah)\",\"Homo sapiens3\",\"my study\"\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies(getGraphDb()).exportStudy(study, writer, true);
                assertThat(writer.toString(), is("\"original target taxon name\",\"unmatched normalized target taxon name\",\"study\"\n" +
                        "\"Canis lupus other\",\"Canis lupus other\",\"my study\"\n"
                ));
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus", externalId);
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        StudyExportUnmatchedTaxaForStudies exporter = new StudyExportUnmatchedSourceTaxaForStudies(getGraphDb());
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "unmatched.csv");
        String expectedMetaTable = exporter.getMetaTablePrefix() + "unmatched.csv" + exporter.getMetaTableSuffix();
        assertThat(writer.toString(), is(expectedMetaTable));
    }

}