package org.eol.globi.tool;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.doi.DOI;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class IndexInteractionsNeo4j2Test extends GraphDBNeo4jTestCase {

    @Test
    public void indexInteractions() throws StudyImporterException {
        TaxonIndex taxonIndex = getTaxonIndex();
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/wiki/Nanopubs
        StudyImpl study = new StudyImpl("some study", new DOI("123.23", "222"), "some study citation");
        DatasetWithResourceMapping dataset = new DatasetWithResourceMapping("some/namespace", URI.create("https://some.uri"),
                new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        NodeFactoryWithDatasetContext factory = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
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

        getInteractionIndexer().index();

        NodeFactoryNeo4j nodeFactoryNeo4j = getNodeFactory();
        StudyImpl study1 = new StudyImpl("some study", new DOI("123.23", "222"), "come citation");
        study1.setOriginatingDataset(new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop())));
        StudyNode someStudy = nodeFactoryNeo4j.getOrCreateStudy(study1);

        assertThat(interaction.getOriginatingDataset().getNamespace(), is(someStudy.getOriginatingDataset().getNamespace()));
        assertThat(interaction.getTitle(), is(someStudy.getTitle()));

        RelationshipType hasParticipant = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);
        Set<Long> ids = new HashSet<>();
        List<Long> idList = new ArrayList<>();

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(someStudy.getUnderlyingNode()), new RelationshipListener() {
            @Override
            public void on(Relationship specimen) {
                assertThat(specimen.getEndNode().hasRelationship(Direction.INCOMING, hasParticipant), Is.is(true));
                Iterable<Relationship> relationships = specimen.getEndNode().getRelationships(hasParticipant, Direction.INCOMING);
                for (Relationship relationship : relationships) {
                    long id = relationship.getStartNode().getId();
                    ids.add(id);
                    idList.add(id);
                }

            }
        });

        assertThat(ids.size(), Is.is(1));
        assertThat(idList.size(), Is.is(2));

        Node interactionNode = getGraphDb().getNodeById(idList.get(0));
        assertTrue(interactionNode.hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.DERIVED_FROM)));
        assertTrue(interactionNode.hasRelationship(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.ACCESSED_AT)));

    }

    protected IndexerNeo4j getInteractionIndexer() {
        return new IndexInteractionsNeo4j2(new GraphServiceFactoryProxy(getGraphDb()));
    }
}