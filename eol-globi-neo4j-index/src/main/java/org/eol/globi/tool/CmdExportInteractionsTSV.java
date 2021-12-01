package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.export.GraphExporterInteractionsTSVImpl;

import java.io.File;

public class CmdExportInteractionsTSV implements Cmd {

    private final GraphServiceFactory factory;
    private File baseDir;

    public CmdExportInteractionsTSV(GraphServiceFactory factory, File baseDir) {
        this.factory = factory;
        this.baseDir = baseDir;
    }

    @Override
    public void run() throws StudyImporterException {
        new GraphExporterInteractionsTSVImpl()
                .export(factory.getGraphService(), baseDir);
    }
}
