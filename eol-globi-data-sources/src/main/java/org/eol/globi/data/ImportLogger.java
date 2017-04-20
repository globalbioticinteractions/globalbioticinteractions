package org.eol.globi.data;

import org.eol.globi.domain.LogContext;

public interface ImportLogger {
    void warn(LogContext study, String message);
    void info(LogContext study, String message);
    void severe(LogContext study, String message);
}
