package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GraphExporterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GraphExporterUtil.class);


    public static void exportInteractionsAndCitations(GraphDatabaseService graphService,
                                                      File baseDir,
                                                      String extension,
                                                      ExportUtil.ValueJoiner joiner,
                                                      String neo4jVersion) throws StudyImporterException {
        File formatBaseDir = new File(baseDir, extension);
        try {
            FileUtils.forceMkdir(formatBaseDir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create export dir [" + formatBaseDir.getAbsolutePath() + "]", e);
        }
        exportSupportingInteractions(
                graphService,
                formatBaseDir,
                "interactions." + extension + ".gz",
                joiner,
                RelTypes.CLASSIFIED_AS,
                neo4jVersion);

        exportSupportingInteractions(
                graphService,
                formatBaseDir,
                "verbatim-interactions." + extension + ".gz",
                joiner,
                RelTypes.ORIGINALLY_DESCRIBED_AS,
                neo4jVersion);

        exportRefutedInteractions(
                graphService,
                formatBaseDir,
                "refuted-interactions." + extension + ".gz",
                joiner,
                RelTypes.CLASSIFIED_AS,
                neo4jVersion);

        exportRefutedInteractions(
                graphService,
                formatBaseDir,
                "refuted-verbatim-interactions." + extension + ".gz",
                joiner,
                RelTypes.ORIGINALLY_DESCRIBED_AS,
                neo4jVersion);

        exportCitations(
                graphService,
                formatBaseDir,
                "citations." + extension + ".gz",
                joiner,
                neo4jVersion);

        exportDatasetNamespaces(
                graphService,
                formatBaseDir,
                "datasets." + extension + ".gz",
                joiner,
                neo4jVersion);
    }

    private static void exportSupportingInteractions(GraphDatabaseService graphService, File baseDir, String filename, ExportUtil.ValueJoiner joiner, RelTypes taxonRelation, String neo4jVersion) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportFlatInteractions(joiner, filename, taxonRelation, neo4jVersion)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime(TimeUnit.SECONDS) + "s.");
    }

    private static void exportCitations(GraphDatabaseService graphService,
                                        File baseDir, String filename,
                                        ExportUtil.ValueJoiner joiner,
                                        String neo4jVersion) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportCitations(joiner, filename, neo4jVersion)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime(TimeUnit.SECONDS) + "s.");
    }

    private static void exportDatasetNamespaces(GraphDatabaseService graphService,
                                                File baseDir, String filename,
                                                ExportUtil.ValueJoiner joiner, String neo4jVersion) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportNamespaces(joiner, filename, neo4jVersion)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime(TimeUnit.SECONDS) + "s.");
    }

    private static void exportRefutedInteractions(
            GraphDatabaseService graphService,
            File baseDir, String filename,
            ExportUtil.ValueJoiner joiner,
            RelTypes taxonRelation,
            String neo4jVersion) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportFlatInteractions(joiner, filename, taxonRelation, neo4jVersion)
                .setArgumentType(RelTypes.REFUTES)
                .setArgumentTypeId(PropertyAndValueDictionary.REFUTES)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime(TimeUnit.SECONDS) + "s.");
    }

}
