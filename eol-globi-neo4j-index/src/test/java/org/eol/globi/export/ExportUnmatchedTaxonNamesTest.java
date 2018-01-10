package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
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
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(null, getGraphDb());

        String title = "my study\"";
        String citation = "citation my study";
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl(title, "my first source", null, citation));

        taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null));
        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, human());
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Canis lupus", null));
        Specimen preySpecimen6 = nodeFactory.createSpecimen(study, dog());
        predatorSpecimen.interactsWith(preySpecimen6, InteractType.ATE);
        Specimen preySpecimen5 = nodeFactory.createSpecimen(study, dog());
        predatorSpecimen.interactsWith(preySpecimen5, InteractType.ATE);
        Specimen preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Caniz", null));
        predatorSpecimen.ate(preySpecimen);

        Specimen predatorSpecimen23 = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens2", null));
        Specimen preySpecimen4 = nodeFactory.createSpecimen(study, dog());
        predatorSpecimen23.interactsWith(preySpecimen4, InteractType.ATE);

        Specimen predatorSpecimen22 = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens2", null));
        Specimen preySpecimen3 = nodeFactory.createSpecimen(study, dog());
        predatorSpecimen22.interactsWith(preySpecimen3, InteractType.ATE);

        Study study2 = nodeFactory.getOrCreateStudy(new StudyImpl("my study2", "my source2", null, "citation study2"));
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen(study2, new TaxonImpl("Homo sapiens2", null));
        Specimen preySpecimen2 = nodeFactory.createSpecimen(study2, dog());
        predatorSpecimen21.interactsWith(preySpecimen2, InteractType.ATE);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens3", PropertyAndValueDictionary.NO_MATCH));
        Specimen preySpecimen1 = nodeFactory.createSpecimen(study, dog());
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

    private Taxon dog() {
        Taxon dog = new TaxonImpl("Canis lupus");
        dog.setExternalId("canisLupusId");
        dog.setPath("four five six");
        return dog;
    }

    private Taxon human() {
        TaxonImpl taxon = new TaxonImpl("Homo sapiens");
        taxon.setExternalId("homoSapiensId");
        taxon.setPath("one two three");
        return taxon;
    }

    @Test
    public void exportOnePredatorNoPathButWithSameAs() throws NodeFactoryException, IOException {

        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(null, getGraphDb());

        String citation = "cite, study";
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("my, study", "my first, source", null, citation));

        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapienz", null));
        Taxon humanz = taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapienz", null));
        TaxonImpl taxon = new TaxonImpl("Homo sapiens", "TESTING:123");
        taxon.setPath("one | two | Homo sapiens");
        NodeUtil.connectTaxa(taxon, (TaxonNode)humanz, getGraphDb(), RelTypes.SIMILAR_TO);
        assertNotNull(humanz);
        Specimen preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Caniz", null));
        predatorSpecimen.interactsWith(preySpecimen, InteractType.ATE);

        predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens", null));
        Node synonymNode = ((NodeBacked)taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens Synonym", null))).getUnderlyingNode();
        Node node = ((NodeBacked)taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null))).getUnderlyingNode();
        Transaction tx = getGraphDb().beginTx();
        try {
            node.createRelationshipTo(synonymNode, NodeUtil.asNeo4j(RelTypes.SAME_AS));
            tx.success();
        } finally {
            tx.finish();
        }

        preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Canis", null));
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