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

public class GraphExporterImpl implements GraphExporter {
    private static final Logger LOG = LoggerFactory.getLogger(GraphExporterImpl.class);

    @Override
    public void export(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(baseDir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + baseDir.getAbsolutePath() + "]", e);
        }

        doExport(graphService, baseDir);
    }

    public void doExport(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        LOG.info("site maps generating... ");
        File siteMapDir = new File(baseDir, "sitemap");

        final File citationsDir = new File(siteMapDir, "citations");
        LOG.info("site maps at [" + citationsDir.getAbsolutePath() + "] generating... ");
        new ExporterSiteMapForCitations().export(graphService, citationsDir);
        LOG.info("site maps at [" + citationsDir.getAbsolutePath() + "] generated.");

        final File namesDir = new File(siteMapDir, "names");
        LOG.info("site maps at [" + namesDir.getAbsolutePath() + "] generating... ");
        final GraphExporter exporter = new ExporterSiteMapForNames();
        exporter.export(graphService, namesDir);
        LOG.info("site maps at [" + namesDir.getAbsolutePath() + "] generated.");

        LOG.info("site maps generated... ");

        LOG.info("ncbi linkout files generating... ");
        exportNCBILinkOut(graphService, baseDir);
        LOG.info("ncbi linkout files generated. ");

        exportNames(graphService, baseDir);

        GraphExporterUtil.exportInteractionsAndCitations(
                graphService,
                baseDir,
                "csv",
                new ExportUtil.CsvValueJoiner()
        );

        exportDataOntology(graphService, baseDir);
        exportDarwinCoreAggregatedByStudy(graphService, baseDir);
        exportDarwinCoreAll(graphService, baseDir);
    }

    private void exportNCBILinkOut(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        final File ncbiDir = new File(baseDir, "ncbi-link-out/");
        mkdir(ncbiDir);
        new ExportNCBIIdentityFile().export(graphService, ncbiDir);

        new ExportNCBIResourceFile().export(graphService, new ExportNCBIResourceFile.OutputStreamFactory() {
            @Override
            public OutputStream create(int i) throws IOException {
                return new FileOutputStream(new File(ncbiDir, String.format("resources_%d.xml", i)));
            }
        });
    }

    private void exportNames(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        File taxaDir = new File(baseDir, "taxa");
        mkdir(taxaDir);
        exportNames(graphService, taxaDir, new ExportTaxonMap(), "taxonMap.tsv.gz");
        exportNames(graphService, taxaDir, new ExportTaxonCache(), "taxonCache.tsv.gz");
        //exportNames(studies, baseDir, new ExportUnmatchedTaxonNames(), "taxa/taxonUnmatched.tsv");
    }

    private void mkdir(File dir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + dir + "]", e);
        }
    }

    private void exportNames(GraphDatabaseService graphService, File baseDir, StudyExporter exporter, String filename) throws StudyImporterException {
        try {
            File filePath = new File(baseDir, filename);
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

    private void exportDarwinCoreAggregatedByStudy(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        exportDarwinCoreArchive(graphService,
                new File(baseDir,"aggregatedByStudy"), new HashMap<String, DarwinCoreExporter>() {
                    {
                        put("association.tsv", new ExporterAssociationAggregates());
                        put("occurrence.tsv", new ExporterOccurrenceAggregates());
                        put("references.tsv", new ExporterReferences());
                        put("taxa.tsv", new ExporterTaxaDistinct());
                    }
                }
        );
    }

    private void exportDarwinCoreAll(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        exportDarwinCoreArchive(graphService, new File(baseDir, "all"), new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.tsv", new ExporterAssociations());
                put("occurrence.tsv", new ExporterOccurrences());
                put("references.tsv", new ExporterReferences());
                put("taxa.tsv", new ExporterTaxaDistinct());
                put("measurementOrFact.tsv", new ExporterMeasurementOrFact());
            }
        });
    }

    private void exportDataOntology(GraphDatabaseService graphService, File baseDir) throws StudyImporterException {
        try {
            ExporterRDF studyExporter = new ExporterRDF();
            File exportFile = new File(baseDir, "interactions.nq.gz");
            OutputStreamWriter writer = openStream(exportFile);
            String msg = "writing nquads archive to [" + exportFile + "]";
            LOG.info(msg + "...");
            NodeUtil.findStudies(graphService, node -> {
                try {
                    studyExporter.exportStudy(
                            new StudyNode(node),
                            ExportUtil.AppenderWriter.of(writer, new ExportUtil.NQuadValueJoiner()),
                            true);
                } catch (IOException e) {
                    throw new IllegalStateException("failed to export interactions to [" + exportFile.getAbsolutePath() + "]", e);
                }

            });
            LOG.info(msg + " done.");
            closeStream(exportFile, writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(GraphDatabaseService graphDatabaseService, File baseDir, Map<String, DarwinCoreExporter> exporters) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(baseDir);
            FileWriter darwinCoreMeta = writeMetaHeader(baseDir);
            for (Map.Entry<String, DarwinCoreExporter> exporter : exporters.entrySet()) {
                export(graphDatabaseService, baseDir, exporter.getKey(), exporter.getValue(), darwinCoreMeta);
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

    private FileWriter writeMetaHeader(File baseDir) throws IOException {
        FileWriter darwinCoreMeta = new FileWriter(new File(baseDir, "meta.xml"), false);
        darwinCoreMeta.write("<?xml version=\"1.0\"?>\n" +
                "<archive xmlns=\"http://rs.tdwg.org/dwc/text/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://rs.tdwg.org/dwc/text/  http://services.eol.org/schema/dwca/tdwg_dwc_text.xsd\">\n");
        return darwinCoreMeta;
    }

    private void export(GraphDatabaseService graphService, File exportPath, String filename, DarwinCoreExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        File exportFile = new File(exportPath, filename);
        OutputStreamWriter writer = openStream(exportFile);

        NodeUtil.findStudies(graphService, new NodeListener() {
            final AtomicBoolean isFirst = new AtomicBoolean(true);

            @Override
            public void on(Node node) {
                try {
                    studyExporter.exportStudy(new StudyNode(node),
                            ExportUtil.AppenderWriter.of(writer), isFirst.getAndSet(false));
                } catch (IOException e) {
                    throw new IllegalStateException("failed to export to [" + exportFile.getAbsolutePath() + "]", e);
                }

            }
        });
        closeStream(exportFile, writer);

        LOG.info("darwin core meta file writing... ");
        studyExporter.exportDarwinCoreMetaTable(darwinCoreMeta, filename);
        LOG.info("darwin core meta file written. ");
    }

    private void closeStream(File exportFile, OutputStreamWriter writer) throws IOException {
        writer.flush();
        writer.close();
        LOG.info("export data to [" + exportFile.getAbsolutePath() + "] complete.");
    }

    private OutputStreamWriter openStream(File exportFile) throws IOException {
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(exportFile));
        String exportFilePath = exportFile.getAbsolutePath();
        if (exportFilePath.endsWith(".gz")) {
            fos = new GZIPOutputStream(fos);
        }
        OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        LOG.info("export data to [" + exportFilePath + "] started...");
        return writer;
    }

}
