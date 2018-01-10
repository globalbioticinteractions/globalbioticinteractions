package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class ExportCitations implements GraphExporter {


    public static final String CYPHER_QUERY = "START study = node:studies('*:*') " +
            "RETURN study.externalId? as uri" +
            ", study.citation? as citation";

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        String tsvFilename = "/citations.tsv.gz";
        export(graphService, baseDir, tsvFilename, CYPHER_QUERY);
    }

    void export(GraphDatabaseService graphService, Writer writer) throws IOException {
        ExportCitations.export(graphService, writer, CYPHER_QUERY);
    }

    public static void export(GraphDatabaseService graphService, String baseDir, String tsvFilename, String cypherQuery) throws StudyImporterException {
        try {
            ExportUtil.mkdirIfNeeded(baseDir);
            final FileOutputStream out = new FileOutputStream(baseDir + tsvFilename);
            GZIPOutputStream os = new GZIPOutputStream(out);
            final Writer writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            export(graphService, writer, cypherQuery);
            writer.flush();
            os.finish();
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export citations", e);
        }
    }

    public static void export(GraphDatabaseService graphService, Writer writer, String query) throws IOException {
        HashMap<String, Object> params = new HashMap<String, Object>() {{
        }};
        ExportUtil.writeResults(writer, graphService, query, params, true);
    }

}
