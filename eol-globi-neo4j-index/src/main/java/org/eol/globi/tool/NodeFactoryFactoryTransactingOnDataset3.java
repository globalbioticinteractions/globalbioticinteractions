package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.data.NodeFactoryNeo4j3;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.concurrent.atomic.AtomicReference;

public class NodeFactoryFactoryTransactingOnDataset3 implements NodeFactoryFactory {
    private GraphServiceFactory graphServiceFactory;

    public NodeFactoryFactoryTransactingOnDataset3(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public NodeFactory create(GraphDatabaseService service) {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            NodeFactory nodeFactory = new NodeFactoryNeo4j3(graphService) {
                final AtomicReference<Transaction> tx = new AtomicReference<>();

                @Override
                public Dataset getOrCreateDataset(Dataset dataset) {
                    Transaction transaction = tx.getAndSet(graphServiceFactory.getGraphService().beginTx());
                    if (transaction != null) {
                        transaction.success();
                        transaction.close();
                    }
                    return super.getOrCreateDataset(dataset);
                }

            };
            tx.success();
            return nodeFactory;
        }

    }
}
