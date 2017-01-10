package org.eol.globi.domain;

import org.eol.globi.service.Dataset;

import java.util.List;
import java.util.logging.Level;

public interface Study extends Named {
    String getTitle();

    String getDOI();

    String getCitation();

    void appendLogMessage(String message, Level warning);

    List<LogMessage> getLogMessages();

    String getSource();

    String getSourceId();

    Dataset getOriginatingDataset();
}
