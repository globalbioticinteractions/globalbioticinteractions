package org.eol.globi.tool;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public class TaxonInteractionIndexerNeo4j2Test extends GraphDBNeo4jTestCase {

    @Test
    public void buildTaxonInterIndex() throws StudyImporterException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Homo sapiens", "NCBI:9606"));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Canis lupus", "WORMS:2"));
        human.ate(animal);
        for (int i = 0; i < 10; i++) {
            Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Arius felis", "WORMS:158711"));
            human.ate(fish);
        }

        new NameResolver(
                new GraphServiceFactoryProxy(getGraphDb()),
                getNodeIdCollector(),
                getTaxonIndex()
        ).index();

        IndexerNeo4j taxonInteractionIndexer = new TaxonInteractionIndexer(new GraphServiceFactoryProxy(getGraphDb()), getNodeIdCollector());
        taxonInteractionIndexer.index();

        Taxon homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);

        Iterable<Relationship> rels = ((NodeBacked) homoSapiens).getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE));
        List<String> humanFood = new ArrayList<String>();
        List<Long> counts = new ArrayList<Long>();
        List<String> labels = new ArrayList<>();
        for (Relationship rel : rels) {
            humanFood.add((String) rel.getEndNode().getProperty("name"));
            counts.add((Long) rel.getProperty("count"));
            labels.add((String) rel.getProperty("label"));

        }
        assertThat(humanFood.size(), is(4));
        assertThat(humanFood, hasItems("Arius felis", "Canis lupus"));
        assertThat(counts, hasItems(10L, 1L));
        assertThat(labels, hasItems("eats"));
    }

    @Test
    public void indexNoNameNoMatch() throws NodeFactoryException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Homo sapiens", PropertyAndValueDictionary.NO_MATCH));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Canis lupus", PropertyAndValueDictionary.NO_MATCH));
        human.ate(animal);
        for (int i = 0; i < 10; i++) {
            Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Arius felis", null));
            human.ate(fish);
        }

        new NameResolver(new GraphServiceFactoryProxy(getGraphDb()),
                getNodeIdCollector(),
                getTaxonIndex())
                .index();

        assertNotNull(taxonIndex.findTaxonByName("Homo sapiens"));
//        assertNull(taxonIndex.findTaxonById(PropertyAndValueDictionary.NO_MATCH));
//        assertNull(taxonIndex.findTaxonByName(PropertyAndValueDictionary.NO_NAME));

    }

}
