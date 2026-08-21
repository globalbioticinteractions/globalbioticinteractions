package org.globalbioticinteractions.elton;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.domain.LocationConstant;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.IndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Neo4jIndexUtil {
    private final static Logger LOG = LoggerFactory.getLogger(Neo4jIndexUtil.class);

    public static void createPointLocationIndexIfNotExists(Transaction tx) {
        NodeLabel nodeLabel = NodeLabel.Location;
        List<String> indexNames = new ArrayList<>();
        Iterable<IndexDefinition> taxonIndexes = tx.schema().getIndexes(nodeLabel);
        taxonIndexes.forEach(i -> indexNames.add(i.getName()));
        String indexName = nodeLabel.name();
        if (indexNames.contains(indexName)) {
            LOG.info("found location index [{}]", indexName);
        } else {
            tx.schema()
                    .indexFor(nodeLabel)
                    .withIndexType(IndexType.POINT)
                    .on(LocationConstant.LNGLAT)
                    .withName(indexName)
                    .create();
            LOG.info("created location index [{}]", indexName);
        }
    }

    public static void createIndexIfNotExists(Transaction tx, NodeLabel nodeLabel, String propertyName) {
        List<String> indexNames = new ArrayList<>();
        Iterable<IndexDefinition> taxonIndexes = tx.schema().getIndexes(nodeLabel);
        taxonIndexes.forEach(i -> indexNames.add(i.getName()));
        String indexName = indexNameFor(propertyName, nodeLabel);
        if (indexNames.contains(indexName)) {
            LOG.info("found index [{}]", indexName);
        } else {
            tx
                    .schema()
                    .indexFor(nodeLabel)
                    .on(propertyName)
                    .withIndexType(IndexType.RANGE)
                    .withName(indexName)
                    .create();
            LOG.info("created index [{}]", indexName);
        }
    }

    private static String indexNameFor(String propertyName, NodeLabel nodeLabel) {
        return StringUtils.joinWith("_", nodeLabel.name(), propertyName);
    }
}
