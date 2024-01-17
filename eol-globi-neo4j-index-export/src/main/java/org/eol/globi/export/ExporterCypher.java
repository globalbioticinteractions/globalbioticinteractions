package org.eol.globi.export;

import apoc.ApocConfiguration;
import apoc.export.cypher.ExportCypher;
import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

public class ExporterCypher implements GraphExporter {

    private final String filename;

    public ExporterCypher(String filename) {
        this.filename = filename;
    }


    @Override
    public void export(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        try {
            ApocConfiguration.addToConfig(new TreeMap<String, Object>() {{
                put("export.file.enabled", Boolean.TRUE);
            }});
            new ExportCypher(graphService).all(new File(baseDir, filename).getAbsolutePath(), null);
        } catch (IOException e) {
            throw new StudyImporterException("cypher export failed", e);
        }

    }

}
