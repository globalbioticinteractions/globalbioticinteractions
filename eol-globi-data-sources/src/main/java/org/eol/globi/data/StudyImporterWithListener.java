package org.eol.globi.data;

public abstract class StudyImporterWithListener extends BaseStudyImporter {

    private InteractionListener interactionListener;

    public StudyImporterWithListener(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        this.interactionListener = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
    }

    public InteractionListener getInteractionListener() {
        return interactionListener;
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }
}
