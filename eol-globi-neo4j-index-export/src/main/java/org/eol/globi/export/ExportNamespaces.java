package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public class ExportNamespaces implements GraphExporter {

    private final String filename;
    private final ExportUtil.ValueJoiner joiner;

    public ExportNamespaces(ExportUtil.ValueJoiner joiner, String filename) {
        this.filename = filename;
        this.joiner = joiner;
    }

    public static final String QUERY = "MATCH (dataset:Dataset) " +
            "RETURN distinct(dataset.namespace) as namespace " +
            "ORDER BY namespace";

    @Override
    public void export(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        ExportUtil.export(
                graphService,
                new File(baseDir, filename),
                QUERY,
                joiner
        );
    }

}
