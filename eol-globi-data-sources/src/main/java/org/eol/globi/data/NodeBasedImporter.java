package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;

public abstract class NodeBasedImporter extends BaseDatasetImporter implements IndexEventListener {
    protected final ParserFactory parserFactory;
    private final NodeFactory nodeFactory;
    protected InteractionListener interactionListener = null;

    NodeBasedImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
        this.parserFactory = parserFactory;
    }

    protected NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    protected ParserFactory getParserFactory() {
        return parserFactory;
    }

    public InteractionListener getInteractionListener() {
        return this.interactionListener;
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    @Override
    public void notifyInteractionRecordIndexed() throws StudyImporterException {
        InteractionListener listener = getInteractionListener();
        if (listener != null) {
            listener.on(null);
        }
    }
}
