package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb) {
        super(graphDb);
        createDatasetConstraint(getGraphDb());
    }

    @Override
    protected Node createDatasetNode(Dataset dataset) {
        Node datasetNode = getGraphDb().createNode(NodeLabel.Dataset);
        return super.createDatasetNode(dataset, datasetNode);
    }

    private void createDatasetConstraint(GraphDatabaseService graphDb) {
        try (Transaction transaction = graphDb.beginTx()) {
            if (!graphDb
                    .schema()
                    .getConstraints(NodeLabel.Dataset)
                    .iterator()
                    .hasNext()) {

                graphDb
                        .schema()
                        .constraintFor(NodeLabel.Dataset)
                        .assertPropertyIsUnique(DatasetConstant.NAMESPACE)
                        .create();
            }
            transaction.success();
        }
    }

    @Override
    protected Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {

            Node node = getGraphDb()
                    .findNode(NodeLabel.Dataset,
                            DatasetConstant.NAMESPACE,
                            originatingDataset.getNamespace());

            Node datasetNode = node == null
                    ? createDatasetNode(originatingDataset)
                    : node;

            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }

}

