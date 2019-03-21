package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

public class ExportCitations implements GraphExporter {

    private final String filename;
    private final ExportUtil.ValueJoiner joiner;

    public ExportCitations(ExportUtil.ValueJoiner joiner, String filename) {
        this.filename = filename;
        this.joiner = joiner;
    }

    public static final String CYPHER_QUERY = "CYPHER 1.9 START study = node:studies('*:*') " +
            "RETURN study.doi? as doi, study.citation? as citation";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        ExportUtil.export(graphService, baseDir, filename, CYPHER_QUERY, joiner);
    }

}
