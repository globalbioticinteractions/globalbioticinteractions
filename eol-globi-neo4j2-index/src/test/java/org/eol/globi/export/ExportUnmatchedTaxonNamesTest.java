package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;

public class ExportUnmatchedTaxonNamesTest extends GraphDBTestCase {

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(null, getGraphDb());

        String title = "my study\"";
        String citation = "citation my study";

        NodeFactory nodeFactory = nodeFactoryWithDataset();

        StudyNode study = (StudyNode) nodeFactory.getOrCreateStudy(new StudyImpl(title, null, citation));


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

        Study study2 = nodeFactory.getOrCreateStudy(new StudyImpl("my study2", null, "citation study2"));
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen(study2, new TaxonImpl("Homo sapiens2", null));
        Specimen preySpecimen2 = nodeFactory.createSpecimen(study2, dog());
        predatorSpecimen21.interactsWith(preySpecimen2, InteractType.ATE);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens3", PropertyAndValueDictionary.NO_MATCH));
        Specimen preySpecimen1 = nodeFactory.createSpecimen(study, dog());
        predatorSpecimen2.interactsWith(preySpecimen1, InteractType.ATE);
        resolveNames();

        StringWriter writer = new StringWriter();
        new ExportUnmatchedTaxonNames().exportStudy(study, ExportUtil.AppenderWriter.of(writer), true);
        String actual = writer.toString();
        assertThat(actual, startsWith("unmatched taxon name\tunmatched taxon id\tname status\tsimilar to taxon name\tsimilar to taxon path\tsimilar to taxon id\tstudy\tsource"));
        assertThat(actual, containsString("\nCaniz\t\t\t\t\t\tcitation my study\t<some:archive>"));
        assertThat(actual, containsString("\nHomo sapiens2\t\t\t\t\t\tcitation my study\t<some:archive>"));
        assertThat(actual, containsString("\nHomo sapiens3\tno:match\t\t\t\t\tcitation my study\t<some:archive>"));
    }

    public NodeFactoryWithDatasetContext nodeFactoryWithDataset() throws NodeFactoryException {
        Dataset dataset = nodeFactory.getOrCreateDataset(
                new DatasetWithResourceMapping("some/namespace",
                        URI.create("some:archive"),
                        new ResourceServiceLocalAndRemote(in -> in)));

        return new NodeFactoryWithDatasetContext(nodeFactory, dataset);
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
        NodeFactory nodeFactory = nodeFactoryWithDataset();
        StudyNode study = (StudyNode) nodeFactory.getOrCreateStudy(new StudyImpl("my, study", null, citation));

        Specimen predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapienz", null));
        Taxon humanz = taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapienz", null));
        TaxonImpl taxon = new TaxonImpl("Homo sapiens", "TESTING:123");
        taxon.setPath("one | two | Homo sapiens");
        NodeUtil.connectTaxa(taxon, (TaxonNode) humanz, getGraphDb(), RelTypes.SIMILAR_TO);
        assertNotNull(humanz);
        Specimen preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Caniz", null));
        predatorSpecimen.interactsWith(preySpecimen, InteractType.ATE);

        predatorSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Homo sapiens", null));
        Node synonymNode = ((NodeBacked) taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens Synonym", null))).getUnderlyingNode();
        Node node = ((NodeBacked) taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null))).getUnderlyingNode();
        node.createRelationshipTo(synonymNode, NodeUtil.asNeo4j(RelTypes.SAME_AS));

        preySpecimen = nodeFactory.createSpecimen(study, new TaxonImpl("Canis", null));
        predatorSpecimen.ate(preySpecimen);
        resolveNames();
        StringWriter writer = new StringWriter();
        new ExportUnmatchedTaxonNames().exportStudy(study, ExportUtil.AppenderWriter.of(writer), true);
        String actual = writer.toString();
        assertThat(actual, startsWith("unmatched taxon name\tunmatched taxon id\tname status\tsimilar to taxon name\tsimilar to taxon path\tsimilar to taxon id\tstudy\tsource"));
        assertThat(actual, containsString("\nHomo sapienz\t\t\tHomo sapiens\tone | two | Homo sapiens\tTESTING:123\tcite, study\t<some:archive>"));
        assertThat(actual, containsString("\nCaniz\t\t\t\t\t\tcite, study\t<some:archive>"));
        assertThat(actual, containsString("\nCanis\t\t\t\t\t\tcite, study\t<some:archive>"));
    }

}