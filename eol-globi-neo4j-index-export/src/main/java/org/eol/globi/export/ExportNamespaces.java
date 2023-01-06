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

    public static final String CYPHER_QUERY = "CYPHER 2.3 START dataset = node:datasets('*:*') " +
            "RETURN distinct(dataset.namespace) as namespace " +
            "ORDER BY namespace";

    public static final String CYPHER_QUERY_V3 = "MATCH (dataset:Dataset) " +
            "RETURN distinct(dataset.namespace) as namespace " +
            "ORDER BY namespace";

    @Override
    public void export(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        String cypherQuery = "2".equalsIgnoreCase(neo4jVersion) ? CYPHER_QUERY : CYPHER_QUERY_V3;
        ExportUtil.export(graphService, new File(baseDir, filename), cypherQuery, joiner);
    }

}
