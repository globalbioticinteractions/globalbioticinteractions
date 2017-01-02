package org.eol.globi.domain;

import java.util.List;
import java.util.logging.Level;

public interface Study extends Named {
    String getTitle();

    String getSource();

    String getDOI();

    String getCitation();

    void appendLogMessage(String message, Level warning);

    List<LogMessage> getLogMessages();
}
