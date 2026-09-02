package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class InteractionListenerBatching implements InteractionListener {
    public static final int BATCH_SIZE_DEFAULT = 1000;
    final InteractionListener interactionListener;
    final AtomicLong counter;
    private final NodeFactory nodeFactory;
    private final int batchSize;

    public InteractionListenerBatching(NodeFactory nodeFactory,
                                       InteractionListener interactionListener) {
        this.nodeFactory = nodeFactory;
        this.batchSize = BATCH_SIZE_DEFAULT;
        this.interactionListener = interactionListener;
        counter = new AtomicLong(batchSize);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        if (interactionListener != null && interaction != null) {
            interactionListener.on(interaction);
        }

        if (counter.decrementAndGet() <= 0) {
            nodeFactory.startNextBatchUpdate();
            counter.set(batchSize);
        }
    }
}
