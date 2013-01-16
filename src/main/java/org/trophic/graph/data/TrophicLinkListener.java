package org.trophic.graph.data;

import org.trophic.graph.domain.Study;

interface TrophicLinkListener {

    public void newLink(Study study, String predatorName, String preyName);
}
