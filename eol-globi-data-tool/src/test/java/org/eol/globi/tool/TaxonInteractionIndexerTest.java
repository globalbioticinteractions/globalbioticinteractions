package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class TaxonInteractionIndexerTest extends GraphDBTestCase {

    @Test
    public void buildTaxonInterIndex() throws NodeFactoryException, PropertyEnricherException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Homo sapiens", null));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Canis lupus", "EOL:1"));
        human.ate(animal);
        for (int i=0; i< 10; i++) {
            Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Arius felis", null));
            human.ate(fish);
        }

        assertNull(taxonIndex.findTaxonById("EOL:1"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        new NameResolver(getGraphDb()).resolve();
        new TaxonInteractionIndexer(getGraphDb()).index();

        TaxonNode homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);

        Iterable<Relationship> rels = homoSapiens.getUnderlyingNode().getRelationships(Direction.OUTGOING, InteractType.ATE);
        List<String> humanFood = new ArrayList<String>();
        List<Long> counts = new ArrayList<Long>();
        List<String> labels = new ArrayList<>();
        for (Relationship rel : rels) {
            humanFood.add((String) rel.getEndNode().getProperty("name"));
            counts.add((Long) rel.getProperty("count"));
            labels.add((String)rel.getProperty("label"));

        }
        assertThat(humanFood.size(), is(4));
        assertThat(humanFood, hasItems("Ariopsis felis", "Animalia"));
        assertThat(counts, hasItems(10L, 1L));
        assertThat(labels, hasItems("eats"));
    }

}
