package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonIndexImpl;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedTaxaForStudiesTest extends GraphDBTestCase {

    public static final String EXPECTED_HEADER = "original taxon name,original taxon external id,unmatched normalized taxon name,unmatched normalized taxon external id,study,source";

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) {
                Taxon taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four five six");
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        NodeFactory factory = factory(taxonEnricher);
        String title = "my study\"";
        Study study = factory.getOrCreateStudy2(title, "my first source", null);

        factory.getOrCreateTaxon("Homo sapiens");
        Specimen predatorSpecimen = factory.createSpecimen(study, "Homo sapiens");
        factory.getOrCreateTaxon("Canis lupus");
        Specimen preySpecimen6 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen.interactsWith(preySpecimen6, InteractType.ATE);
        Specimen preySpecimen5 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen.interactsWith(preySpecimen5, InteractType.ATE);
        Specimen preySpecimen = factory.createSpecimen(study, "Caniz");
        predatorSpecimen.ate(preySpecimen);

        Specimen predatorSpecimen23 = factory.createSpecimen(study, "Homo sapiens2");
        Specimen preySpecimen4 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen23.interactsWith(preySpecimen4, InteractType.ATE);

        Specimen predatorSpecimen22 = factory.createSpecimen(study, "Homo sapiens2");
        Specimen preySpecimen3 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen22.interactsWith(preySpecimen3, InteractType.ATE);

        Study study2 = factory.getOrCreateStudy2("my study2", "my source2", null);
        Specimen predatorSpecimen21 = factory.createSpecimen(study2, "Homo sapiens2");
        Specimen preySpecimen2 = nodeFactory.createSpecimen(study2, "Canis lupus");
        predatorSpecimen21.interactsWith(preySpecimen2, InteractType.ATE);

        Specimen predatorSpecimen2 = factory.createSpecimen(study, "Homo sapiens3", PropertyAndValueDictionary.NO_MATCH);
        Specimen preySpecimen1 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen2.interactsWith(preySpecimen1, InteractType.ATE);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "Caniz,,Caniz,no:match,\"my study\"\"\",my first source\n" +
                        "Homo sapiens2,,Homo sapiens2,no:match,\"my study\"\"\",my first source\n" +
                        "Homo sapiens3,no:match,Homo sapiens3,no:match,\"my study\"\"\",my first source\n"
        ));
    }

    @Test
    public void exportOnePredatorNoPathButWithSameAs() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) {
                TaxonImpl taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                String externalId = taxon.getExternalId() == null
                        ? PropertyAndValueDictionary.NO_MATCH : taxon.getExternalId();
                return TaxonUtil.taxonToMap(new TaxonImpl(taxon.getName(), externalId));
            }

            @Override
            public void shutdown() {

            }
        };
        NodeFactory factory = factory(taxonEnricher);
        Study study = factory.getOrCreateStudy2("my study", "my first source", null);

        Specimen predatorSpecimen = factory.createSpecimen(study, "Homo sapienz");
        assertNotNull(factory.getOrCreateTaxon("Homo sapienz"));
        Specimen preySpecimen = factory.createSpecimen(study, "Caniz");
        predatorSpecimen.interactsWith(preySpecimen, InteractType.ATE);

        predatorSpecimen = factory.createSpecimen(study, "Homo sapiens");
        Node synonymNode = factory.getOrCreateTaxon("Homo sapiens Synonym").getUnderlyingNode();
        Node node = factory.getOrCreateTaxon("Homo sapiens").getUnderlyingNode();
        Transaction tx = getGraphDb().beginTx();
        try {
            node.createRelationshipTo(synonymNode, RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }

        preySpecimen = factory.createSpecimen(study, "Canis");
        predatorSpecimen.ate(preySpecimen);

        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "Homo sapienz,,Homo sapienz,no:match,my study,my first source\n" +
                        "Caniz,,Caniz,no:match,my study,my first source\n" +
                        "Canis,,Canis,no:match,my study,my first source\n"
        ));
    }

    private NodeFactoryImpl factory(PropertyEnricher enricher) {
        return new NodeFactoryImpl(getGraphDb(), new TaxonIndexImpl(enricher, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb()));
    }

}