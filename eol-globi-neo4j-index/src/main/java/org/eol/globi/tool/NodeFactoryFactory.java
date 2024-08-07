package org.eol.globi.tool;

import org.eol.globi.data.NodeFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public interface NodeFactoryFactory {
    NodeFactory create(GraphDatabaseService service, final File cacheDir);
}
