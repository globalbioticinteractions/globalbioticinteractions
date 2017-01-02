package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ExportUnmatchedTaxonNamesTest extends GraphDBTestCase {

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
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());

        String title = "my study\"";
        StudyNode study = nodeFactory.getOrCreateStudy2(title, "my first source", null);
        study.setCitationWithTx("citation my study");

        taxonIndex.getOrCreateTaxon("Homo sapiens");
        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, "Homo sapiens", "TEST:1234");
        taxonIndex.getOrCreateTaxon("Canis lupus");
        SpecimenNode preySpecimen6 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen.interactsWith(preySpecimen6, InteractType.ATE);
        SpecimenNode preySpecimen5 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen.interactsWith(preySpecimen5, InteractType.ATE);
        SpecimenNode preySpecimen = nodeFactory.createSpecimen(study, "Caniz");
        predatorSpecimen.ate(preySpecimen);

        Specimen predatorSpecimen23 = nodeFactory.createSpecimen(study, "Homo sapiens2");
        SpecimenNode preySpecimen4 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen23.interactsWith(preySpecimen4, InteractType.ATE);

        Specimen predatorSpecimen22 = nodeFactory.createSpecimen(study, "Homo sapiens2");
        SpecimenNode preySpecimen3 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen22.interactsWith(preySpecimen3, InteractType.ATE);

        StudyNode study2 = nodeFactory.getOrCreateStudy2("my study2", "my source2", null);
        study2.setCitationWithTx("citation study2");
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen(study2, "Homo sapiens2");
        SpecimenNode preySpecimen2 = nodeFactory.createSpecimen(study2, "Canis lupus");
        predatorSpecimen21.interactsWith(preySpecimen2, InteractType.ATE);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen(study, "Homo sapiens3", PropertyAndValueDictionary.NO_MATCH);
        SpecimenNode preySpecimen1 = nodeFactory.createSpecimen(study, "Canis lupus");
        predatorSpecimen2.interactsWith(preySpecimen1, InteractType.ATE);
        resolveNames();

        StringWriter writer = new StringWriter();
        new ExportUnmatchedTaxonNames().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("unmatched taxon name\tunmatched taxon id\tname status\tsimilar to taxon name\tsimilar to taxon path\tsimilar to taxon id\tstudy\tsource" +
                        "\nCaniz\t\t\t\t\t\tcitation my study\tmy first source" +
                        "\nHomo sapiens2\t\t\t\t\t\tcitation my study\tmy first source" +
                        "\nHomo sapiens3\tno:match\t\t\t\t\tcitation my study\tmy first source"
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
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());

        StudyNode study = nodeFactory.getOrCreateStudy2("my, study", "my first, source", null);
        study.setCitationWithTx("cite, study");

        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, "Homo sapienz");
        TaxonNode humanz = taxonIndex.getOrCreateTaxon("Homo sapienz");
        TaxonImpl taxon = new TaxonImpl("Homo sapiens", "TESTING:123");
        taxon.setPath("one | two | Homo sapiens");
        NodeUtil.connectTaxa(taxon, humanz, getGraphDb(), RelTypes.SIMILAR_TO);
        assertNotNull(humanz);
        SpecimenNode preySpecimen = nodeFactory.createSpecimen(study, "Caniz");
        predatorSpecimen.interactsWith(preySpecimen, InteractType.ATE);

        predatorSpecimen = nodeFactory.createSpecimen(study, "Homo sapiens");
        Node synonymNode = taxonIndex.getOrCreateTaxon("Homo sapiens Synonym").getUnderlyingNode();
        Node node = taxonIndex.getOrCreateTaxon("Homo sapiens").getUnderlyingNode();
        Transaction tx = getGraphDb().beginTx();
        try {
            node.createRelationshipTo(synonymNode, RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }

        preySpecimen = nodeFactory.createSpecimen(study, "Canis");
        predatorSpecimen.ate(preySpecimen);
        resolveNames();
        StringWriter writer = new StringWriter();
        new ExportUnmatchedTaxonNames().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("unmatched taxon name\tunmatched taxon id\tname status\tsimilar to taxon name\tsimilar to taxon path\tsimilar to taxon id\tstudy\tsource" +
                        "\nHomo sapienz\t\t\tHomo sapiens\tone | two | Homo sapiens\tTESTING:123\tcite, study\tmy first, source" +
                        "\nCaniz\t\t\t\t\t\tcite, study\tmy first, source" +
                        "\nCanis\t\t\t\t\t\tcite, study\tmy first, source"
        ));
    }

}