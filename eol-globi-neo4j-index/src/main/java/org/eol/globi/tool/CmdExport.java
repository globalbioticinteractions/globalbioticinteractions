package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.export.GraphExporterImpl;

import java.io.File;

public class CmdExport implements Cmd {

    private final GraphServiceFactory factory;
    private File baseDir;

    public CmdExport(GraphServiceFactory factory) {
        this(factory, new File("."));
    }

    public CmdExport(GraphServiceFactory factory, File baseDir) {
        this.factory = factory;
        this.baseDir = baseDir;
    }

    @Override
    public void run() throws StudyImporterException {
        new GraphExporterImpl()
                .export(factory.getGraphService(), baseDir);
    }
}
