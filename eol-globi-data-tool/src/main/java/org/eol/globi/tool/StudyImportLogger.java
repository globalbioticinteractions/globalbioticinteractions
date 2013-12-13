package org.eol.globi.tool;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.domain.Study;

import java.util.logging.Level;

public class StudyImportLogger implements ImportLogger {
    private final NodeFactory factory;

    public StudyImportLogger(NodeFactory factory) {
        this.factory = factory;
    }

    @Override
    public void warn(Study study, String message) {
        createMsg(study, message, Level.WARNING);
    }

    @Override
    public void info(Study study, String message) {
        createMsg(study, message, Level.INFO);
    }

    @Override
    public void severe(Study study, String message) {
        createMsg(study, message, Level.SEVERE);
    }

    private void createMsg(Study study, String message, Level warning) {
        if (null != study) {
            study.appendLogMessage(message, warning);
        }
    }

}
