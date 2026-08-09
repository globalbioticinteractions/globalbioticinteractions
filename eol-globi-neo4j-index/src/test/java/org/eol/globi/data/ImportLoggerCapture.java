package org.eol.globi.data;

import org.eol.globi.domain.LogContext;

import java.util.List;

public class ImportLoggerCapture implements ImportLogger {
    private final List<String> msgs;

    public ImportLoggerCapture(List<String> msgs) {
        this.msgs = msgs;
    }

    @Override
    public void warn(LogContext ctx, String message) {
        msgs.add(message);
    }

    @Override
    public void info(LogContext ctx, String message) {
        msgs.add(message);
    }

    @Override
    public void severe(LogContext ctx, String message) {
        msgs.add(message);
    }
}
