package org.eol.globi.data;

import org.eol.globi.domain.Study;

interface TrophicLinkListener {

    public void newLink(Study study, String predatorName, String preyName, String country, String state, String locality);
}
