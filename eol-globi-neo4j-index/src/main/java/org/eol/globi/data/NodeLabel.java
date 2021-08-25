package org.eol.globi.data;

import org.neo4j.graphdb.Label;

public enum NodeLabel implements Label {
    Reference,
    ExternalId, Season, Dataset
}

