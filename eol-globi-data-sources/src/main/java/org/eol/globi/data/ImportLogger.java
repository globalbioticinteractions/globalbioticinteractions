package org.eol.globi.data;

import org.eol.globi.domain.Study;

public interface ImportLogger {
    void warn(Study study, String message);
    void info(Study study, String message);
    void severe(Study study, String message);
}
