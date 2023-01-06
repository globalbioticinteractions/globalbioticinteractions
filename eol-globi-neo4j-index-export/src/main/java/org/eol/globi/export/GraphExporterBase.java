package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;

public abstract class GraphExporterBase implements GraphExporter {

    @Override
    public void export(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(baseDir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + baseDir.getAbsolutePath() + "]", e);
        }

        doExport(graphService, baseDir, neo4jVersion);
    }

    abstract public void doExport(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException;


}
