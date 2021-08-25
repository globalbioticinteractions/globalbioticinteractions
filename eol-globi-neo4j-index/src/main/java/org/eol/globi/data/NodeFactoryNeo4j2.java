package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class NodeFactoryNeo4j2 extends NodeFactoryNeo4j {

    private final Index<Node> datasets;


    public NodeFactoryNeo4j2(GraphDatabaseService graphDb) {
        super(graphDb);
        this.datasets = NodeUtil.forNodes(graphDb, "datasets");
    }

    @Override
    protected Node createDatasetNode(Dataset dataset) {
        Node datasetNode = getGraphDb().createNode();
        super.createDatasetNode(dataset, datasetNode);
        datasets.add(datasetNode, DatasetConstant.NAMESPACE, dataset.getNamespace());
        return datasetNode;
    }

    @Override
    protected Dataset getOrCreateDatasetNoTx(Dataset originatingDataset) {
        Dataset datasetCreated = null;
        if (originatingDataset != null && StringUtils.isNotBlank(originatingDataset.getNamespace())) {
            IndexHits<Node> datasetHits = datasets.get(DatasetConstant.NAMESPACE, originatingDataset.getNamespace());

            Node datasetNode = datasetHits.hasNext()
                    ? datasetHits.next()
                    : createDatasetNode(originatingDataset);

            datasetCreated = new DatasetNode(datasetNode);
        }
        return datasetCreated;
    }


}

