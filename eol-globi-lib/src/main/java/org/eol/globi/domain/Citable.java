package org.eol.globi.domain;

import org.globalbioticinteractions.doi.DOI;

public interface Citable extends WithId {

    /**
     *
     * @return DOI if available, null otherwise
     */
    DOI getDOI();

    /**
     *
     * @return free text citation string
     */
    String getCitation();

}
