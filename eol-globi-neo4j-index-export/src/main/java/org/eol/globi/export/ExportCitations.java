package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

public class ExportCitations implements GraphExporter {


    public static final String CYPHER_QUERY = "START study = node:studies('*:*') " +
            "RETURN study.doi? as doi, study.citation? as citation";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        String tsvFilename = "/citations.tsv.gz";
        ExportUtil.export(graphService, baseDir, tsvFilename, CYPHER_QUERY);
    }

}
