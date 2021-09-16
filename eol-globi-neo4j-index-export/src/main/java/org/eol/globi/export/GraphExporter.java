package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public interface GraphExporter {
    void export(GraphDatabaseService graphService, File baseDir) throws StudyImporterException;
}
