package org.eol.globi.domain;

import org.globalbioticinteractions.dataset.Dataset;

public interface Study extends Citable, Named, LogContext {
    String getTitle();
    Dataset getOriginatingDataset();
}
