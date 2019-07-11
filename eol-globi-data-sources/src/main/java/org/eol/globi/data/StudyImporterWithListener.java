package org.eol.globi.data;

import org.eol.globi.service.GeoNamesService;

public abstract class StudyImporterWithListener extends BaseStudyImporter {

    private InteractionListener interactionListener;

    public StudyImporterWithListener(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        this.interactionListener = initListener(nodeFactory);
    }

    public InteractionListenerImpl initListener(NodeFactory nodeFactory) {
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
        this.interactionListener = initListener(nodeFactory);
    }

    @Override
    public void setGeoNamesService(GeoNamesService geoNamesService) {
        super.setGeoNamesService(geoNamesService);
        this.interactionListener = initListener(nodeFactory);
    }
}
