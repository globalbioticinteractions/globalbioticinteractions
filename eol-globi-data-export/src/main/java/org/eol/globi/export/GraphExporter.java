package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

public interface GraphExporter {
    void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException;
}
