package org.eol.globi.domain;

import org.eol.globi.service.Dataset;
import org.globalbioticinteractions.doi.DOI;

public interface Study extends Named, LogContext {
    String getTitle();

    DOI getDOI();

    String getCitation();

    String getSource();

    String getSourceId();

    Dataset getOriginatingDataset();
}
