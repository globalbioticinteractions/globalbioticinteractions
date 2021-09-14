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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

public class GraphExporterImpl implements GraphExporter {
    private static final Logger LOG = LoggerFactory.getLogger(GraphExporterImpl.class);

    @Override
    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + baseDir + "]", e);
        }

        LOG.info("site maps generating... ");
        File siteMapDir = new File(baseDir, "sitemap");

        final File citations = new File(siteMapDir, "citations");
        LOG.info("site maps at [" + citations.getAbsolutePath() + "] generating... ");
        new ExporterSiteMapForCitations().export(graphService, citations.getAbsolutePath());
        LOG.info("site maps at [" + citations.getAbsolutePath() + "] generated.");

        final File names = new File(siteMapDir, "names");
        LOG.info("site maps at [" + names.getAbsolutePath() + "] generating... ");
        final GraphExporter exporter = new ExporterSiteMapForNames();
        exporter.export(graphService, names.getAbsolutePath());
        LOG.info("site maps at [" + names.getAbsolutePath() + "] generated.");

        LOG.info("site maps generated... ");

        LOG.info("ncbi linkout files generating... ");
        exportNCBILinkOut(graphService, baseDir);
        LOG.info("ncbi linkout files generated. ");

        exportNames(graphService, baseDir);

        exportInteractionsAndCitations(
                graphService,
                baseDir,
                "tsv",
                new ExportUtil.TsvValueJoiner()
        );

        exportInteractionsAndCitations(
                graphService,
                baseDir,
                "csv",
                new ExportUtil.CsvValueJoiner()
        );

        exportDataOntology(graphService, baseDir);
        exportDarwinCoreAggregatedByStudy(graphService, baseDir);
        exportDarwinCoreAll(graphService, baseDir);
    }

    private void exportInteractionsAndCitations(GraphDatabaseService graphService,
                                                String baseDir,
                                                String extension,
                                                ExportUtil.ValueJoiner joiner) throws StudyImporterException {
        File formatBaseDir = new File(baseDir, extension);
        try {
            FileUtils.forceMkdir(formatBaseDir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create export dir [" + formatBaseDir.getAbsolutePath() + "]", e);
        }
        exportSupportingInteractions(
                graphService,
                formatBaseDir.getAbsolutePath(),
                new File(formatBaseDir, "interactions." + extension + ".gz").getAbsolutePath(),
                joiner);

        exportRefutedInteractions(
                graphService,
                formatBaseDir.getAbsolutePath(),
                new File(formatBaseDir,"refuted-interactions." + extension + ".gz").getAbsolutePath(),
                joiner);

        exportCitations(
                graphService,
                formatBaseDir.getAbsolutePath(),
                new File(formatBaseDir, "citations." + extension + ".gz").getAbsolutePath(),
                joiner);
    }

    private void exportSupportingInteractions(GraphDatabaseService graphService, String baseDir, String filename, ExportUtil.ValueJoiner joiner) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportFlatInteractions(joiner, filename)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime() / 1000 + "s.");
    }

    private void exportCitations(GraphDatabaseService graphService, String baseDir, String filename, ExportUtil.ValueJoiner joiner) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportCitations(new ExportUtil.TsvValueJoiner(), filename)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime() / 1000 + "s.");
    }

    private void exportRefutedInteractions(GraphDatabaseService graphService, String baseDir, String filename, ExportUtil.ValueJoiner joiner) throws StudyImporterException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("[" + filename + "] generating... ");
        new ExportFlatInteractions(joiner, filename)
                .setArgumentType(RelTypes.REFUTES)
                .setArgumentTypeId(PropertyAndValueDictionary.REFUTES)
                .export(graphService, baseDir);
        stopWatch.stop();
        LOG.info("[" + filename + "] generated in " + stopWatch.getTime() / 1000 + "s.");
    }

    private void exportNCBILinkOut(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        final String ncbiDir = baseDir + "ncbi-link-out/";
        mkdir(ncbiDir);
        new ExportNCBIIdentityFile().export(graphService, ncbiDir);

        new ExportNCBIResourceFile().export(graphService, new ExportNCBIResourceFile.OutputStreamFactory() {
            @Override
            public OutputStream create(int i) throws IOException {
                return new FileOutputStream(ncbiDir + String.format("resources_%d.xml", i));
            }
        });
    }

    private void exportNames(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        mkdir(baseDir, "taxa");
        exportNames(graphService, baseDir, new ExportTaxonMap(), "taxa/taxonMap.tsv.gz");
        exportNames(graphService, baseDir, new ExportTaxonCache(), "taxa/taxonCache.tsv.gz");
        //exportNames(studies, baseDir, new ExportUnmatchedTaxonNames(), "taxa/taxonUnmatched.tsv");
    }

    private void mkdir(String baseDir, String subdirName) throws StudyImporterException {
        mkdir(baseDir + subdirName);
    }

    private void mkdir(String dir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(dir));
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + dir + "]", e);
        }
    }

    private void exportNames(GraphDatabaseService graphService, String baseDir, StudyExporter exporter, String filename) throws StudyImporterException {
        try {
            String filePath = baseDir + filename;
            OutputStreamWriter writer = openStream(filePath);
            NodeUtil.findStudies(graphService, new NodeListener() {
                final AtomicBoolean isFirst = new AtomicBoolean(true);

                @Override
                public void on(Node node) {
                    boolean includeHeader = isFirst.getAndSet(false);
                    try {
                        exporter.exportStudy(new StudyNode(node), ExportUtil.AppenderWriter.of(writer), includeHeader);
                    } catch (IOException e) {
                        throw new IllegalStateException("failed to export names to [" + filePath + "]");
                    }

                }
            });
            closeStream(filePath, writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export unmatched source taxa", e);
        }
    }

    private void exportDarwinCoreAggregatedByStudy(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        exportDarwinCoreArchive(graphService,
                baseDir + "aggregatedByStudy/", new HashMap<String, DarwinCoreExporter>() {
                    {
                        put("association.tsv", new ExporterAssociationAggregates());
                        put("occurrence.tsv", new ExporterOccurrenceAggregates());
                        put("references.tsv", new ExporterReferences());
                        put("taxa.tsv", new ExporterTaxaDistinct());
                    }
                }
        );
    }

    private void exportDarwinCoreAll(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        exportDarwinCoreArchive(graphService, baseDir + "all/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.tsv", new ExporterAssociations());
                put("occurrence.tsv", new ExporterOccurrences());
                put("references.tsv", new ExporterReferences());
                put("taxa.tsv", new ExporterTaxaDistinct());
                put("measurementOrFact.tsv", new ExporterMeasurementOrFact());
            }
        });
    }

    private void exportDataOntology(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        try {
            ExporterRDF studyExporter = new ExporterRDF();
            String exportPath = baseDir + "interactions.nq.gz";
            OutputStreamWriter writer = openStream(exportPath);
            String msg = "writing nquads archive to [" + exportPath + "]";
            LOG.info(msg + "...");
            NodeUtil.findStudies(graphService, node -> {
                try {
                    studyExporter.exportStudy(
                            new StudyNode(node),
                            ExportUtil.AppenderWriter.of(writer, new ExportUtil.NQuadValueJoiner()),
                            true);
                } catch (IOException e) {
                    throw new IllegalStateException("failed to export interactions to [" + exportPath + "]", e);
                }

            });
            LOG.info(msg + " done.");
            closeStream(exportPath, writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(GraphDatabaseService graphDatabaseService, String pathPrefix, Map<String, DarwinCoreExporter> exporters) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(pathPrefix));
            FileWriter darwinCoreMeta = writeMetaHeader(pathPrefix);
            for (Map.Entry<String, DarwinCoreExporter> exporter : exporters.entrySet()) {
                export(graphDatabaseService, pathPrefix, exporter.getKey(), exporter.getValue(), darwinCoreMeta);
            }
            writeMetaFooter(darwinCoreMeta);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private void writeMetaFooter(FileWriter darwinCoreMeta) throws IOException {
        darwinCoreMeta.write("</archive>");
        darwinCoreMeta.flush();
        darwinCoreMeta.close();
    }

    private FileWriter writeMetaHeader(String pathPrefix) throws IOException {
        FileWriter darwinCoreMeta = new FileWriter(pathPrefix + "meta.xml", false);
        darwinCoreMeta.write("<?xml version=\"1.0\"?>\n" +
                "<archive xmlns=\"http://rs.tdwg.org/dwc/text/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://rs.tdwg.org/dwc/text/  http://services.eol.org/schema/dwca/tdwg_dwc_text.xsd\">\n");
        return darwinCoreMeta;
    }

    private void export(GraphDatabaseService graphService, String exportPath, String filename, DarwinCoreExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        String exportPath1 = exportPath + filename;
        OutputStreamWriter writer = openStream(exportPath1);

        NodeUtil.findStudies(graphService, new NodeListener() {
            final AtomicBoolean isFirst = new AtomicBoolean(true);

            @Override
            public void on(Node node) {
                try {
                    studyExporter.exportStudy(new StudyNode(node),
                            ExportUtil.AppenderWriter.of(writer), isFirst.getAndSet(false));
                } catch (IOException e) {
                    throw new IllegalStateException("failed to export to [" + exportPath1 + "]", e);
                }

            }
        });
        closeStream(exportPath1, writer);

        LOG.info("darwin core meta file writing... ");
        studyExporter.exportDarwinCoreMetaTable(darwinCoreMeta, filename);
        LOG.info("darwin core meta file written. ");
    }

    private void closeStream(String exportPath, OutputStreamWriter writer) throws IOException {
        writer.flush();
        writer.close();
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }

    private OutputStreamWriter openStream(String exportPath) throws IOException {
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(exportPath));
        if (exportPath.endsWith(".gz")) {
            fos = new GZIPOutputStream(fos);
        }
        OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        return writer;
    }

}
