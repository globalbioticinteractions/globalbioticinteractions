package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonPropertyLookupServiceException;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LinkerTest extends GraphDBTestCase {

    @Test
    public void twoTaxa() throws NodeFactoryException, TaxonPropertyLookupServiceException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Ariopsis felis");

        new Linker().linkTaxa(getGraphDb());

        assertHasOther("Homo sapiens");
        assertHasOther("Ariopsis felis");

    }

    private void assertHasOther(String name) throws NodeFactoryException {
        TaxonNode taxon1 = nodeFactory.findTaxon(name);
        assertThat(taxon1.getName(), is(name));
        Iterable<Relationship> rels = taxon1.getUnderlyingNode().getRelationships(RelTypes.SAME_AS, Direction.OUTGOING);
        assertTrue("expected relationship for " + name, rels.iterator().hasNext());
    }
}
