package org.eol.globi.domain;

import org.eol.globi.service.Dataset;

public interface Study extends Named, LogContext {
    String getTitle();

    String getDOI();

    String getCitation();

    String getSource();

    String getSourceId();

    Dataset getOriginatingDataset();
}
