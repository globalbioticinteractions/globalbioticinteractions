package org.eol.globi.tool;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.Set;

public class LinkerTestUtil {

    public static Set<String> sameAsCountForNode(RelTypes relType, NodeBacked taxon1) {
        Set<String> externalIds = new HashSet<>();
        Iterable<Relationship> rels = taxon1.getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(relType), Direction.OUTGOING);
        for (Relationship rel : rels) {
            externalIds.add(new TaxonNode(rel.getEndNode()).getExternalId());
        }
        return externalIds;
    }
}
