package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public class ExportNamespaces implements GraphExporter {

    private final String filename;
    private final ExportUtil.ValueJoiner joiner;
    private final String neo4jVersion;

    public ExportNamespaces(ExportUtil.ValueJoiner joiner, String filename, String neo4jVersion) {
        this.filename = filename;
        this.joiner = joiner;
        this.neo4jVersion = neo4jVersion;
    }

    public static final String CYPHER_QUERY_V3 = "MATCH (dataset:Dataset) " +
            "RETURN distinct(dataset.namespace) as namespace " +
            "ORDER BY namespace";

    @Override
    public void export(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        ExportUtil.export(
                graphService,
                new File(baseDir, filename),
                CYPHER_QUERY_V3,
                joiner
        );
    }

}
