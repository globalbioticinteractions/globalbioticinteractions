package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public class GraphExporterInteractionsTSVImpl extends GraphExporterBase {

    private final String neo4jVersion;

    public GraphExporterInteractionsTSVImpl(String neo4jVersion) {
        this.neo4jVersion = neo4jVersion;
    }

    @Override
    public void doExport(GraphDatabaseService graphService, File baseDir, String neo4jVersion) throws StudyImporterException {
        GraphExporterUtil.exportInteractionsAndCitations(
                graphService,
                baseDir,
                "tsv",
                new ExportUtil.TsvValueJoiner(),
                neo4jVersion
        );
    }

}
