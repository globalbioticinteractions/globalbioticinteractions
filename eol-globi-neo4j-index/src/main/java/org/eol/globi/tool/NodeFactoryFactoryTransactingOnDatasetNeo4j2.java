package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryNeo4j2;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.globalbioticinteractions.dataset.Dataset;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class NodeFactoryFactoryTransactingOnDatasetNeo4j2 implements NodeFactoryFactory {
    private GraphServiceFactory graphServiceFactory;
    private final int TRANSACTION_BATCH_SIZE_DEFAULT = 10000;


    public NodeFactoryFactoryTransactingOnDatasetNeo4j2(GraphServiceFactory graphServiceFactory) {
        this.graphServiceFactory = graphServiceFactory;
    }

    @Override
    public NodeFactory create(GraphDatabaseService service) {
        GraphDatabaseService graphService = graphServiceFactory.getGraphService();
        try (Transaction tx = graphService.beginTx()) {
            NodeFactory nodeFactory = new NodeFactoryNeo4j2(graphService) {
                final AtomicReference<Transaction> tx = new AtomicReference<>();
                final AtomicBoolean closing = new AtomicBoolean(false);
                final AtomicLong counter = new AtomicLong(0);

                @Override
                public Dataset getOrCreateDataset(Dataset dataset) throws NodeFactoryException {
                    if (closing.get()) {
                        throw new IllegalStateException("cannot create a dataset on closing node factorySkipBOM");
                    } else {
                        startBatchTransactionIfNeeded();
                    }
                    return super.getOrCreateDataset(dataset);
                }

                void startBatchTransactionIfNeeded() {
                    tx.getAndUpdate(transaction -> {
                        if (counter.getAndIncrement() % TRANSACTION_BATCH_SIZE_DEFAULT == 0) {
                            if (transaction != null) {
                                transaction.success();
                                transaction.close();
                                transaction = null;
                            }
                        }
                        return transaction == null
                                ? beginTx()
                                : transaction;
                    });
                }

                private Transaction beginTx() {
                    return graphServiceFactory.getGraphService().beginTx();
                }

                @Override
                public SpecimenNode createSpecimen(Study study, Taxon taxon, RelTypes... types) throws NodeFactoryException {
                    startBatchTransactionIfNeeded();
                    return super.createSpecimen(study, taxon, types);
                }


                    @Override
                public void close() {
                    tx.getAndUpdate(tx -> {
                        closing.set(true);
                        if (tx != null) {
                            tx.success();
                            tx.close();
                        }
                        return null;
                    });
                }
            };
            tx.success();
            return nodeFactory;
        }

    }
}
