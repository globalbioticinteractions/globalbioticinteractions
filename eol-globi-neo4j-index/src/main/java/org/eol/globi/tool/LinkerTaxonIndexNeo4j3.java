package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.util.NodeIdCollectorNeo4j3;

public class LinkerTaxonIndexNeo4j3 implements IndexerNeo4j {

    public LinkerTaxonIndexNeo4j3(GraphServiceFactory factory) {
    }

    public LinkerTaxonIndexNeo4j3(GraphServiceFactoryProxy factory, NodeIdCollectorNeo4j3 nodeIdCollectorNeo4j3) {

    }

    @Override
    public void index() throws StudyImporterException {

    }
}
