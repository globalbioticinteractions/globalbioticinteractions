package org.eol.globi.data;

import org.neo4j.graphdb.Label;

public enum NodeLabel implements Label {
    Reference,
    ExternalId,
    Season,
    Location,
    Environment,
    Dataset,
    Specimen,
    Taxon
}

