package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.taxon.TaxonIndexNeo4j;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.util.NodeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class TaxonInteractionIndexerTest extends GraphDBTestCase {

    private PropertyEnricher taxonEnricher;

    @Before
    public void init() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @After
    public void shutdown() {
        taxonEnricher.shutdown();
    }

    @Test
    public void buildTaxonInterIndex() throws NodeFactoryException, PropertyEnricherException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Homo sapiens", null));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Canis lupus", "EOL:1"));
        human.ate(animal);
        for (int i=0; i< 10; i++) {
            Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null, null)), new TaxonImpl("Arius felis", null));
            human.ate(fish);
        }

        assertNull(taxonIndex.findTaxonById("EOL:1"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        final TaxonNameCorrector taxonNameCorrector = new TaxonNameCorrector();
        new NameResolver(getGraphDb(), new TaxonIndexNeo4j(taxonEnricher, taxonNameCorrector, getGraphDb())).resolve();
        new TaxonInteractionIndexer(getGraphDb()).index();

        Taxon homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);

        Iterable<Relationship> rels = ((NodeBacked)homoSapiens).getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE));
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
