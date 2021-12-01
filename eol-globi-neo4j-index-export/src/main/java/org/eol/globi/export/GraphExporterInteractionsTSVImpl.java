package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeListener;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

public class GraphExporterInteractionsTSVImpl implements GraphExporter {
    private static final Logger LOG = LoggerFactory.getLogger(GraphExporterInteractionsTSVImpl.class);

    @Override
    public void export(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(baseDir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + baseDir.getAbsolutePath() + "]", e);
        }

        doExport(graphService, baseDir);
    }

    private void doExport(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        GraphExporterUtil.exportInteractionsAndCitations(
                graphService,
                baseDir,
                "tsv",
                new ExportUtil.TsvValueJoiner()
        );
    }

}
