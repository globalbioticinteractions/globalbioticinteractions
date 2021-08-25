package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class NodeFactoryNeo4j2 extends NodeFactoryNeo4j {

    private final Index<Node> datasets;
    private final Index<Node> studies;



    public NodeFactoryNeo4j2(GraphDatabaseService graphDb) {
        super(graphDb);
        this.datasets = NodeUtil.forNodes(graphDb, "datasets");
        this.studies = NodeUtil.forNodes(graphDb, "studies");
    }

    @Override
    Node createStudyNode() {
        return getGraphDb().createNode();
    }

    @Override
    void indexReferenceNode(StudyNode studyNode) {
        studies.add(studyNode.getUnderlyingNode(), StudyConstant.TITLE, studyNode.getTitle());
        studies.add(studyNode.getUnderlyingNode(), StudyConstant.TITLE_IN_NAMESPACE, getTitleInNamespace(studyNode));

    }

    @Override
    protected StudyNode findStudy(Study study) {
        String titleNS = getTitleInNamespace(study);
        final IndexHits<Node> nodes = studies.get(StudyConstant.TITLE_IN_NAMESPACE, titleNS);
        Node foundStudyNode = nodes != null ? nodes.getSingle() : null;
        return foundStudyNode == null ? null : new StudyNode(foundStudyNode);
    }

    @Override
    protected void indexDatasetNode(Dataset dataset, Node datasetNode) {
        datasets.add(datasetNode, DatasetConstant.NAMESPACE, dataset.getNamespace());
    }

    @Override
    protected Node createDatasetNode() {
        return getGraphDb().createNode();
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

