package org.eol.globi.tool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.CypherUtil;
import org.eol.globi.util.InteractUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IndexInteractions implements Linker {
    private static final Log LOG = LogFactory.getLog(IndexInteractions.class);

    private static final RelationshipType HAS_PARTICIPANT = NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT);
    public static final RelationshipType[] INTERACTION_TYPES = NodeUtil.asNeo4j(InteractType.values());
    private final GraphDatabaseService graphDb;
    private int batchSize;

    public IndexInteractions(GraphDatabaseService graphDb) {
        this(graphDb, 20);
    }

    public IndexInteractions(GraphDatabaseService graphDb, int batchSize) {
        this.graphDb = graphDb;
        this.batchSize = batchSize;
    }

    @Override
    public void link() {
        ExecutionEngine engine = new ExecutionEngine(graphDb);
        engine.execute("START dataset = node:datasets('*:*')\n" +
                "MATCH dataset<-[:IN_DATASET]-study-[:COLLECTED]->specimen" +
                        ", specimen-[i:" + InteractUtil.allInteractionsCypherClause() + "]->otherSpecimen\n" +
                "WHERE not(specimen-[:HAS_PARTICIPANT]->()) " +
                "AND not(specimen-[:HAS_PARTICIPANT]->()) " +
                "AND not(has(i.inverted))\n" +
                "CREATE specimen<-[:HAS_PARTICIPANT]-interaction-[:DERIVED_FROM]->study" +
                ", interaction-[:ACCESSED_AT]->dataset" +
                ", otherSpecimen<-[:HAS_PARTICIPANT]-interaction\n" +
                "RETURN distinct(id(dataset))");
    }

}
