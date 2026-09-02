package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NodeFactoryFactoryTransactingOnDataset implements NodeFactoryFactory {
    private GraphServiceFactory graphServiceFactory;

    public NodeFactoryFactoryTransactingOnDataset(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public NodeFactory create(GraphDatabaseService service, final File cacheDir) {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            NodeFactory nodeFactory = new NodeFactoryNeo4j(graphService) {
                final AtomicReference<Transaction> tx = new AtomicReference<>();
                final AtomicBoolean closing = new AtomicBoolean(false);

                @Override
                public Dataset getOrCreateDataset(Dataset dataset) throws NodeFactoryException {
                    if (closing.get()) {
                        throw new IllegalStateException("cannot create a dataset on closing node factorySkipBOM");
                    } else {
                        Transaction transaction = tx.get();
                        if (transaction != null) {
                            transaction.commit();
                            transaction.close();
                        }
                        tx.set(graphServiceFactory.getGraphService().beginTx());
                    }
                    return super.getOrCreateDataset(dataset);
                }

                @Override
                public void close() {
                    closing.set(true);
                    Transaction lastTx = tx.getAndSet(null);
                    if (lastTx != null) {
                        lastTx.commit();
                        lastTx.close();
                    }
                }

            };
            tx.commit();
            return nodeFactory;
        }

    }
}
