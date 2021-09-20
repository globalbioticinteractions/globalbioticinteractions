package org.eol.globi.process;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;

import java.util.Map;

public abstract class InteractionProcessorAbstract implements InteractionProcessor {
    protected final InteractionListener listener;
    protected final ImportLogger logger;

    public InteractionProcessorAbstract(InteractionListener listener, ImportLogger logger) {
        this.listener = listener;
        this.logger = logger;
    }

    @Override
    public void emit(Map<String, String> interaction) throws StudyImporterException {
        if (listener != null) {
            listener.on(interaction);
        }
    }
}
