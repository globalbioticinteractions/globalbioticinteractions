package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonEnricher;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedSourceTaxaForStudiesTest extends GraphDBTestCase {

    public static final String EXPECTED_HEADER = "original source taxon name,original source external id,unmatched normalized source taxon name,unmatched normalized source external id,study,source";

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final TaxonEnricher taxonEnricher = new TaxonEnricher() {
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
        };
        NodeFactory factory = factory(taxonEnricher);
        String title = "my study\"";
        Study study = factory.getOrCreateStudy(title, "my first source", null);

        factory.getOrCreateTaxon("Homo sapiens");
        Specimen predatorSpecimen = factory.createSpecimen("Homo sapiens");
        factory.getOrCreateTaxon("Canis lupus");
        addCanisLupus(predatorSpecimen);
        addCanisLupus(predatorSpecimen);
        Specimen preySpecimen = factory.createSpecimen("Caniz");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen23 = factory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen23);
        study.collected(predatorSpecimen23);
        Specimen predatorSpecimen22 = factory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen22);
        study.collected(predatorSpecimen22);

        Study study2 = factory.getOrCreateStudy("my study2", "my source2", null);
        Specimen predatorSpecimen21 = factory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen21);
        study2.collected(predatorSpecimen21);

        Specimen predatorSpecimen2 = factory.createSpecimen("Homo sapiens3", PropertyAndValueDictionary.NO_MATCH);
        addCanisLupus(predatorSpecimen2);
        study.collected(predatorSpecimen2);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "Homo sapiens2,,Homo sapiens2,no:match,\"my study\\\"\",my first source\n" +
                        "Homo sapiens3,no:match,Homo sapiens3,no:match,\"my study\\\"\",my first source\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("original target taxon name,original target external id,unmatched normalized target taxon name,unmatched normalized target external id,study,source" + "\n" +
                        "Caniz,,Caniz,no:match,\"my study\\\"\",my first source\n"
        ));
    }

    @Test
    public void exportOnePredatorNoPathButWithSameAs() throws NodeFactoryException, IOException {
        final TaxonEnricher taxonEnricher = new TaxonEnricher() {
            @Override
            public void enrich(Taxon taxon) {

            }
        };
        NodeFactory factory = factory(taxonEnricher);
        Study study = factory.getOrCreateStudy("my study", "my first source", null);

        Specimen predatorSpecimen = factory.createSpecimen("Homo sapienz");
        Specimen preySpecimen = factory.createSpecimen("Caniz");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        predatorSpecimen = factory.createSpecimen("Homo sapiens");
        Node synonymNode = factory.getOrCreateTaxon("Homo sapiens Synonym").getUnderlyingNode();
        Node node = factory.getOrCreateTaxon("Homo sapiens").getUnderlyingNode();
        Transaction tx = getGraphDb().beginTx();
        try {
            node.createRelationshipTo(synonymNode, RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }

        preySpecimen = factory.createSpecimen("Canis");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "Homo sapienz,,Homo sapienz,no:match,my study,my first source\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("original target taxon name,original target external id,unmatched normalized target taxon name,unmatched normalized target external id,study,source" + "\n" +
                        "Caniz,,Caniz,no:match,my study,my first source\n" +
                        "Canis,,Canis,no:match,my study,my first source\n"
        ));
    }

    private NodeFactory factory(TaxonEnricher taxonEnricher) {
        return new NodeFactory(getGraphDb(), new TaxonServiceImpl(taxonEnricher, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb()));
    }

    private void addCanisLupus(Specimen predatorSpecimen) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

}