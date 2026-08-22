package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerImpl;
import org.eol.globi.service.GeoNamesService;
import org.globalbioticinteractions.dataset.Dataset;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DatasetImporterWithListener extends NodeBasedImporter {

    private InteractionListener interactionListener = null;

    public DatasetImporterWithListener(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    private InteractionListener initListener(NodeFactory nodeFactory) {
        Dataset dataset = getDataset();
        return initListener(nodeFactory, dataset);
    }

    private InteractionListener initListener(NodeFactory nodeFactory, Dataset dataset) {
        int batchSize = 10000;
        return new InteractionListener() {
            final InteractionListenerImpl interactionListener1 = new InteractionListenerImpl(
                    nodeFactory,
                    getGeoNamesService(),
                    getLogger(),
                    dataset);
            final AtomicLong counter = new AtomicLong(batchSize);
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactionListener1.on(interaction);
                if (counter.decrementAndGet() <= 0) {
                    nodeFactory.startNextBatchUpdate();
                    counter.set(batchSize);
                }
            }
        };
    }

    public InteractionListener getInteractionListener() {
        if (interactionListener == null) {
            interactionListener = initListener(getNodeFactory());
        }
        return interactionListener;
    }
    
    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    @Override
    public void setLogger(ImportLogger importLogger) {
        super.setLogger(importLogger);
        reinitializeListenerIfNeeded();
    }

    private void reinitializeListenerIfNeeded() {
        if (interactionListener != null) {
            initListener(getNodeFactory());
        }
    }

    @Override
    public void setGeoNamesService(GeoNamesService geoNamesService) {
        super.setGeoNamesService(geoNamesService);
        reinitializeListenerIfNeeded();
    }

    @Override
    public void setDataset(Dataset dataset) {
        super.setDataset(dataset);
        reinitializeListenerIfNeeded();
    }
}
