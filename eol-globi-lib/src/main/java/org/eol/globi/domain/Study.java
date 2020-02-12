package org.eol.globi.domain;

import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.doi.DOI;

public interface Study extends Named, LogContext {
    String getTitle();

    DOI getDOI();

    String getCitation();

    String getSource();

    String getSourceId();

    Dataset getOriginatingDataset();
}
