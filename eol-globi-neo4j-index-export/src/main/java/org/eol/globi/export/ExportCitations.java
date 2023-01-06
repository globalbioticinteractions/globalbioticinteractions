package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public class ExportCitations implements GraphExporter {

    private final String filename;
    private final ExportUtil.ValueJoiner joiner;
    private final String neo4jVersion;

    public ExportCitations(ExportUtil.ValueJoiner joiner, String filename, String neo4jVersion) {
        this.filename = filename;
        this.joiner = joiner;
        this.neo4jVersion = neo4jVersion;
    }

    public static final String CYPHER_QUERY = "CYPHER 2.3 START study = node:studies('*:*') " +
            "RETURN study.doi as doi, study.citation as citation";

    public static final String CYPHER_QUERY_V3 = "MATCH (study:Reference) " +
            "RETURN study.doi as doi, study.citation as citation";

    @Override
    public void export(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        String cypherQuery = "2".equals(this.neo4jVersion) ? CYPHER_QUERY : CYPHER_QUERY_V3;
        ExportUtil.export(graphService, new File(baseDir, filename), cypherQuery, joiner);
    }

}
