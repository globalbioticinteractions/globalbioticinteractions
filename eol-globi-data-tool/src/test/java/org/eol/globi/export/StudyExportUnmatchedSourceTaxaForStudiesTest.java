package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedSourceTaxaForStudiesTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        nodeFactory.setTaxonEnricher(new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four five six");
                }
            }
        });
        Study study = nodeFactory.createStudy("my study");
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Canis lupus");
        addCanisLupus(predatorSpecimen);
        addCanisLupus(predatorSpecimen);
        Specimen preySpecimen = nodeFactory.createSpecimen("Caniz");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen23 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen23);
        study.collected(predatorSpecimen23);
        Specimen predatorSpecimen22 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen22);
        study.collected(predatorSpecimen22);

        Study study2 = nodeFactory.createStudy("my study2");
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen21);
        study2.collected(predatorSpecimen21);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen("Homo sapiens3", PropertyAndValueDictionary.NO_MATCH);
        addCanisLupus(predatorSpecimen2);
        study.collected(predatorSpecimen2);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"original source taxon name\",\"unmatched normalized source external id\",\"unmatched normalized source taxon name\",\"study\"\n" +
                "\"Homo sapiens2\",,\"Homo sapiens2\",\"my study\"\n" +
                "\"Homo sapiens3\",,\"Homo sapiens3\",\"my study\"\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(study, writer, true);
                assertThat(writer.toString(), is("\"original target taxon name\",\"unmatched normalized target external id\",\"unmatched normalized target taxon name\",\"study\"\n" +
                        "\"Caniz\",,\"Caniz\",\"my study\"\n"
                ));
    }

    private void addCanisLupus(Specimen predatorSpecimen) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

    @Test
    public void darwinCoreMetaTable() throws IOException {
        StudyExportUnmatchedTaxaForStudies exporter = new StudyExportUnmatchedSourceTaxaForStudies();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "unmatched.csv");
        String expectedMetaTable = exporter.getMetaTablePrefix() + "unmatched.csv" + exporter.getMetaTableSuffix();
        assertThat(writer.toString(), is(expectedMetaTable));
    }

}