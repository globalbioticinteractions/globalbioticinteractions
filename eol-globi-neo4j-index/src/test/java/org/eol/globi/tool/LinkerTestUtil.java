package org.eol.globi.tool;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class LinkerTestUtil {
    public static Collection<String> assertHasOther(String name, int expectedCount, TaxonIndex taxonIndex, RelTypes relType) throws NodeFactoryException {

        Taxon taxon1 = taxonIndex.findTaxonByName(name);
        assertThat(taxon1, is(notNullValue()));
        assertThat(taxon1.getName(), is(name));
        Set<String> externalIds = sameAsCountForNode(relType, (NodeBacked) taxon1);
        assertThat("expected [" + expectedCount + "] relationships for [" + name + "]: [" + externalIds.toString() + "]", externalIds.size(), is(expectedCount));
        return externalIds;
    }

    public static Set<String> sameAsCountForNode(RelTypes relType, NodeBacked taxon1) {
        Set<String> externalIds =  new HashSet<>();
        Iterable<Relationship> rels = taxon1.getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(relType), Direction.OUTGOING);
        for (Relationship rel : rels) {
            externalIds.add(new TaxonNode(rel.getEndNode()).getExternalId());
        }
        return externalIds;
    }
}
