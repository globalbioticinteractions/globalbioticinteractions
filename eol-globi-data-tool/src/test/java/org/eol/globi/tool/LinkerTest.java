package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.opentree.OpenTreeTaxonIndex;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class LinkerTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Ariopsis felis");
        nodeFactory.getOrCreateTaxon("Canis lupus");

        new LinkerGlobalNames().link(getGraphDb());

        assertHasOther("Homo sapiens", 4);
        assertHasOther("Canis lupus", 4);
        assertHasOther("Ariopsis felis", 5);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Gilippus hostilis");
        nodeFactory.getOrCreateTaxon("Euander lacertosus");

        new LinkerGlobalNames().link(getGraphDb());

        assertHasOther("Euander lacertosus", 2);
        assertHasOther("Gilippus hostilis", 2);

    }

    @Test
    public void frogs() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        assertHasOther("Anura", 4);
    }

    @Test
    public void homoSapiensOTT() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Homo sapiens", 5, "770315");
    }

    @Test
    public void ariopsisFelis() throws NodeFactoryException, PropertyEnricherException {
        assertOTTLink("Ariopsis felis", 6, "139650");
    }

    protected void assertOTTLink(String name, int expectedCount, String ottId) throws NodeFactoryException, PropertyEnricherException {
        OpenTreeTaxonIndex index = null;
        try {
            index = new OpenTreeTaxonIndex(getClass().getResource("taxonomy-small.tsv"));
            nodeFactory.getOrCreateTaxon(name);
            LinkerGlobalNames linkerGlobalNames = new LinkerGlobalNames();
            linkerGlobalNames.link(getGraphDb());
            new LinkerOpenTreeOfLife().linkToOpenTreeOfLife(getGraphDb(), index);
            List<String> externalIds = assertHasOther(name, expectedCount);
            assertThat(externalIds, hasItem(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix() + ottId));
        } finally {
            if (index != null) {
                index.destroy();
            }
        }
    }

    private List<String> assertHasOther(String name, int expectedCount) throws NodeFactoryException {
        List<String> externalIds = new ArrayList<String>();
        TaxonNode taxon1 = nodeFactory.findTaxonByName(name);
        assertThat(taxon1.getName(), is(name));
        Iterable<Relationship> rels = taxon1.getUnderlyingNode().getRelationships(RelTypes.SAME_AS, Direction.OUTGOING);
        int counter = 0;
        for (Relationship rel : rels) {
            counter++;
            externalIds.add(new TaxonNode(rel.getEndNode()).getExternalId());

        }
        assertThat("expected [" + expectedCount + "] relationships for " + name, counter, is(expectedCount));
        return externalIds;
    }
}
