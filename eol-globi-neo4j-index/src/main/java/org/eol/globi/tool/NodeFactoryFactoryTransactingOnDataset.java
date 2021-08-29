package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NodeFactoryFactoryTransactingOnDataset implements NodeFactoryFactory {
    private GraphServiceFactory graphServiceFactory;

    public NodeFactoryFactoryTransactingOnDataset(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public NodeFactory create(GraphDatabaseService service) {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            NodeFactory nodeFactory = new NodeFactoryNeo4j2(graphService) {
                final AtomicReference<Transaction> tx = new AtomicReference<>();
                final AtomicBoolean closing = new AtomicBoolean(false);

                @Override
                public Dataset getOrCreateDataset(Dataset dataset) {
                    if (!closing.get()) {
                        Transaction transaction = tx.getAndSet(graphServiceFactory.getGraphService().beginTx());
                        if (transaction != null) {
                            transaction.success();
                            transaction.close();
                        }
                    }
                    return super.getOrCreateDataset(dataset);
                }

                @Override
                public void close() {
                    closing.set(true);
                    Transaction lastTx = tx.getAndSet(null);
                    if (lastTx != null) {
                        lastTx.success();
                        lastTx.close();
                    }
                }


            };
            tx.success();
            return nodeFactory;
        }

    }
}
