package org.eol.globi.domain;

import java.util.List;
import java.util.logging.Level;

public interface LogContext {
    void appendLogMessage(String message, Level warning);
    List<LogMessage> getLogMessages();
}
