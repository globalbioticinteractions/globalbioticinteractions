package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;

public class GraphExporterInteractionsTSVImpl extends GraphExporterBase {

    @Override
    public void doExport(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        GraphExporterUtil.exportInteractionsAndCitations(
                graphService,
                baseDir,
                "tsv",
                new ExportUtil.TsvValueJoiner()
        );
    }

}
