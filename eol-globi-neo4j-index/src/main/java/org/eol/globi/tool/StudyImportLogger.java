package org.eol.globi.tool;

import org.eol.globi.data.ImportLogger;
import org.eol.globi.domain.LogContext;

import java.util.logging.Level;

public class StudyImportLogger implements ImportLogger {

    @Override
    public void warn(LogContext ctx, String message) {
        createMsg(ctx, message, Level.WARNING);
    }

    @Override
    public void info(LogContext ctx, String message) {
        createMsg(ctx, message, Level.INFO);
    }

    @Override
    public void severe(LogContext ctx, String message) {
        createMsg(ctx, message, Level.SEVERE);
    }

    private void createMsg(LogContext study, String message, Level warning) {
        if (null != study) {
            study.appendLogMessage(message, warning);
        }
    }

}
