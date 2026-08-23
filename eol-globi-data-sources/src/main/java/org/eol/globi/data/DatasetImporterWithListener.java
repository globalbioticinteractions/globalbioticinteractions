package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerImpl;
import org.eol.globi.service.GeoNamesService;
import org.globalbioticinteractions.dataset.Dataset;

public abstract class DatasetImporterWithListener extends NodeBasedImporter {

    public DatasetImporterWithListener(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    private InteractionListener initListener(NodeFactory nodeFactory) {
        Dataset dataset = getDataset();
        return initListener(nodeFactory, dataset);
    }

    private InteractionListener initListener(NodeFactory nodeFactory, Dataset dataset) {
        return new InteractionListenerBatching(nodeFactory, new InteractionListenerImpl(
                nodeFactory,
                getGeoNamesService(),
                getLogger(),
                dataset));
    }

    @Override
    public InteractionListener getInteractionListener() {
        if (super.getInteractionListener() == null) {
            interactionListener = initListener(getNodeFactory());
        }
        return interactionListener;
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
