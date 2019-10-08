package org.eol.globi.data;

import org.eol.globi.domain.LogContext;

public interface ImportLogger {
    void warn(LogContext ctx, String message);
    void info(LogContext ctx, String message);
    void severe(LogContext ctx, String message);
}
