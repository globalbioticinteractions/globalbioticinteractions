package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j3;
import org.eol.globi.db.GraphServiceFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NodeFactoryFactoryTransactingOnDatasetNeo4j3 implements NodeFactoryFactory {
    private GraphServiceFactory graphServiceFactory;

    public NodeFactoryFactoryTransactingOnDatasetNeo4j3(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public NodeFactory create(GraphDatabaseService service, final File cacheDir) {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            NodeFactory nodeFactory = new NodeFactoryNeo4j3(graphService, cacheDir) {
                final AtomicReference<Transaction> tx = new AtomicReference<>();
                final AtomicBoolean closing = new AtomicBoolean(false);

                @Override
                public Dataset getOrCreateDataset(Dataset dataset) throws NodeFactoryException {
                    if (closing.get()) {
                        throw new IllegalStateException("cannot create a dataset on closing node factorySkipBOM");
                    } else {
                        Transaction transaction = tx.get();
                        if (transaction != null) {
                            transaction.success();
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
