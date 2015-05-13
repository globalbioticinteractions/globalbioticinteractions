package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LinkerTestUtil {
    public static List<String> assertHasOther(String name, int expectedCount, NodeFactory nodeFactory, RelTypes relType) throws NodeFactoryException {
        List<String> externalIds = new ArrayList<String>();
        TaxonNode taxon1 = nodeFactory.findTaxonByName(name);
        assertThat(taxon1, is(notNullValue()));
        assertThat(taxon1.getName(), is(name));
        Iterable<Relationship> rels = taxon1.getUnderlyingNode().getRelationships(relType, Direction.OUTGOING);
        int counter = 0;
        for (Relationship rel : rels) {
            counter++;
            externalIds.add(new TaxonNode(rel.getEndNode()).getExternalId());

        }
        assertThat("expected [" + expectedCount + "] relationships for [" + name + "]: [" + externalIds.toString() + "]", counter, is(expectedCount));
        return externalIds;
    }
}
