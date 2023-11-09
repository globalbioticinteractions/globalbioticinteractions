package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerImpl;
import org.eol.globi.service.GeoNamesService;
import org.globalbioticinteractions.dataset.Dataset;

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
        return new InteractionListenerImpl(
                nodeFactory,
                getGeoNamesService(),
                getLogger(),
                dataset);
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
