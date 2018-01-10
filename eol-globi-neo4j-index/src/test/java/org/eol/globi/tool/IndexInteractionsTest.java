package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class IndexInteractionsTest extends GraphDBTestCase {

    @Test
    public void indexInteractions() throws NodeFactoryException {
        TaxonIndex taxonIndex = getOrCreateTaxonIndex();
        // see https://github.com/jhpoelen/eol-globi-data/wiki/Nanopubs
        StudyImpl study = new StudyImpl("some study", "some source", "http://doi.org/123.23/222", "some study citation");
        NodeFactoryWithDatasetContext factory = new NodeFactoryWithDatasetContext(nodeFactory, new DatasetImpl("some/namespace", URI.create("https://some.uri")));
        Study interaction = factory.getOrCreateStudy(study);
        TaxonImpl donaldTaxon = new TaxonImpl("donald duck", "NCBI:1234");
        Specimen donald = factory.createSpecimen(interaction, donaldTaxon);
        donald.classifyAs(taxonIndex.getOrCreateTaxon(donaldTaxon));
        TaxonImpl mickeyTaxon = new TaxonImpl("mickey mouse", "NCBI:4444");
        Taxon mickeyTaxonNCBI = taxonIndex.getOrCreateTaxon(new TaxonImpl("mickey mouse", "EOL:567"));
        NodeUtil.connectTaxa(mickeyTaxon, (TaxonNode) mickeyTaxonNCBI, getGraphDb(), RelTypes.SAME_AS);
        Specimen mickey = factory.createSpecimen(interaction, mickeyTaxon);
        mickey.classifyAs(taxonIndex.getOrCreateTaxon(mickeyTaxon));

        donald.ate(mickey);

        new IndexInteractions(getGraphDb()).link();

        NodeFactoryNeo4j nodeFactoryNeo4j = new NodeFactoryNeo4j(getGraphDb());
        StudyImpl study1 = new StudyImpl("some study", "some source", null, "come citation");
        study1.setOriginatingDataset(new DatasetImpl("some/namespace", URI.create("some:uri")));
        StudyNode someStudy = nodeFactoryNeo4j.getOrCreateStudy(study1);

        assertThat(interaction.getOriginatingDataset().getNamespace(), is(someStudy.getOriginatingDataset().getNamespace()));
        assertThat(interaction.getTitle(), is(someStudy.getTitle()));

        Iterable<Relationship> specimens = NodeUtil.getSpecimens(someStudy);

        RelationshipType hasParticipant = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);
        Set<Long> ids = new HashSet<>();
        List<Long> idList = new ArrayList<>();
        for (Relationship specimen : specimens) {
            assertThat(specimen.getEndNode().hasRelationship(Direction.INCOMING, hasParticipant), Is.is(true));
            Relationship singleRelationship = specimen.getEndNode().getSingleRelationship(hasParticipant, Direction.INCOMING);
            long id = singleRelationship.getStartNode().getId();
            ids.add(id);
            idList.add(id);
        }

        assertThat(ids.size(), Is.is(1));
        assertThat(idList.size(), Is.is(2));

        Node interactionNode = getGraphDb().getNodeById(idList.get(0));
        assertTrue(interactionNode.hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.DERIVED_FROM)));
        assertTrue(interactionNode.hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ACCESSED_AT)));

    }
}