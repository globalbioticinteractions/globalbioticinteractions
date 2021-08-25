package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyConstant;
import org.eol.globi.domain.StudyNode;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb) {
        super(graphDb);
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Dataset,
                DatasetConstant.NAMESPACE
        );
        createConstraintIfNeeded(
                getGraphDb(),
                NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE
        );
    }

    @Override
    Node createStudyNode() {
        return getGraphDb().createNode(NodeLabel.Reference);
    }

    @Override
    void indexReferenceNode(StudyNode studyNode) {
        // indexing already done via constraint: do nothing
    }

    @Override
    protected Node createDatasetNode() {
        return getGraphDb().createNode(NodeLabel.Dataset);
    }

    @Override
    protected void indexDatasetNode(Dataset dataset, Node datasetNode) {
        // indexing already done via constraint; do nothing
    }

    @Override
    protected StudyNode findStudy(Study study) {
        Node node = getGraphDb().findNode(NodeLabel.Reference,
                StudyConstant.TITLE_IN_NAMESPACE,
                getTitleInNamespace(study));

        return node == null
                ? null
                : new StudyNode(node);
    }


    @Override
    public StudyNode getOrCreateStudy(Study study) {
        Node node;
        try (Transaction tx = getGraphDb().beginTx()) {
            node = getGraphDb()
                    .findNode(NodeLabel.Reference,
                            StudyConstant.TITLE_IN_NAMESPACE,
                            getTitleInNamespace(study));
            tx.success();
        }

        return node == null
                ? createStudy(study)
                : new StudyNode(node);

    }


    private static void createConstraintIfNeeded(GraphDatabaseService graphDb,
                                                 NodeLabel label,
                                                 String propertyName) {
        try (Transaction transaction = graphDb.beginTx()) {
            if (!graphDb
                    .schema()
                    .getConstraints(label)
                    .iterator()
                    .hasNext()) {

                graphDb
                        .schema()
                        .constraintFor(label)
                        .assertPropertyIsUnique(propertyName)
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

