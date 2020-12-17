package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerImpl;
import org.eol.globi.service.GeoNamesService;

public abstract class DatasetImporterWithListener extends NodeBasedImporter {

    private InteractionListener interactionListener;

    public DatasetImporterWithListener(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        this.interactionListener = initListener(nodeFactory);
    }

    private InteractionListener initListener(NodeFactory nodeFactory) {
        return new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
    }

    public InteractionListener getInteractionListener() {
        return interactionListener;
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    @Override
    public void setLogger(ImportLogger importLogger) {
        super.setLogger(importLogger);
        this.interactionListener = initListener(getNodeFactory());
    }

    @Override
    public void setGeoNamesService(GeoNamesService geoNamesService) {
        super.setGeoNamesService(geoNamesService);
        this.interactionListener = initListener(getNodeFactory());
    }
}
