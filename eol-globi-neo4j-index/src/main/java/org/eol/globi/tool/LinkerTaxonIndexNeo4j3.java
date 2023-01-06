package org.eol.globi.tool;

import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.util.NodeIdCollectorNeo4j2;
import org.eol.globi.util.NodeIdCollectorNeo4j3;

public class LinkerTaxonIndexNeo4j3 extends LinkerTaxonIndexNeo4j2 {

    public LinkerTaxonIndexNeo4j3(GraphServiceFactory factory) {
        super(factory, new NodeIdCollectorNeo4j3());
    }

}
