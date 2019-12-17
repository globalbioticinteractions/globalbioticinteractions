package org.eol.globi.domain;

import java.util.logging.Level;

public interface LogContext {
    void appendLogMessage(String message, Level warning);
}
