package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        List<StudyNode> studies = NodeUtil.findAllStudies(graphService);
        LOG.info("ncbi linkout files generating... ");
        exportNCBILinkOut(graphService, baseDir, studies);
        LOG.info("ncbi linkout files generated. ");

        exportNames(baseDir, studies);
        // export to taxa for now, to avoid additional assemblies
        new ExportFlatInteractions(new ExportUtil.TsvValueJoiner(), "interactions.tsv.gz").export(graphService, "tsv");

        new ExportFlatInteractions(new ExportUtil.TsvValueJoiner(), "refuted-interactions.tsv.gz")
                .setArgumentType(RelTypes.REFUTES)
                .setArgumentTypeId(PropertyAndValueDictionary.REFUTES)
                .export(graphService, "tsv");

        new ExportCitations(new ExportUtil.TsvValueJoiner(), "citations.tsv.gz").export(graphService, "tsv");

        new ExportFlatInteractions(new ExportUtil.CsvValueJoiner(), "interactions.csv.gz")
                .export(graphService, "csv");
        new ExportFlatInteractions(new ExportUtil.CsvValueJoiner(), "refuted-interactions.csv.gz")
                .setArgumentType(RelTypes.REFUTES)
                .setArgumentTypeId(PropertyAndValueDictionary.REFUTES)
                .export(graphService, "csv");


        new ExportCitations(new ExportUtil.CsvValueJoiner(), "citations.csv.gz").export(graphService, "csv");

        exportDataOntology(studies, baseDir);
        exportDarwinCoreAggregatedByStudy(baseDir, studies);
        exportDarwinCoreAll(baseDir, studies);
    }

    public void exportNCBILinkOut(GraphDatabaseService graphService, String baseDir, List<StudyNode> studies) throws StudyImporterException {
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

    public void exportNames(String baseDir, List<StudyNode> studies) throws StudyImporterException {
        mkdir(baseDir, "taxa");
        exportNames(studies, baseDir, new ExportTaxonMap(), "taxa/taxonMap.tsv.gz");
        exportNames(studies, baseDir, new ExportTaxonCache(), "taxa/taxonCache.tsv.gz");
        //exportNames(studies, baseDir, new ExportUnmatchedTaxonNames(), "taxa/taxonUnmatched.tsv");
    }

    public void mkdir(String baseDir, String subdirName) throws StudyImporterException {
        mkdir(baseDir + subdirName);
    }

    public void mkdir(String dir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(dir));
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + dir + "]", e);
        }
    }

    private void exportNames(List<StudyNode> studies, String baseDir, StudyExporter exporter, String filename) throws StudyImporterException {
        try {
            String filePath = baseDir + filename;
            OutputStreamWriter writer = openStream(filePath);
            for (StudyNode study : studies) {
                boolean includeHeader = studies.indexOf(study) == 0;
                exporter.exportStudy(study, ExportUtil.AppenderWriter.of(writer), includeHeader);
            }
            closeStream(filePath, writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export unmatched source taxa", e);
        }
    }

    private void exportDarwinCoreAggregatedByStudy(String baseDir, List<StudyNode> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies,
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

    private void exportDarwinCoreAll(String baseDir, List<StudyNode> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies, baseDir + "all/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.tsv", new ExporterAssociations());
                put("occurrence.tsv", new ExporterOccurrences());
                put("references.tsv", new ExporterReferences());
                put("taxa.tsv", new ExporterTaxaDistinct());
                put("measurementOrFact.tsv", new ExporterMeasurementOrFact());
            }
        });
    }

    private void exportDataOntology(List<StudyNode> studies, String baseDir) throws StudyImporterException {
        try {
            ExporterRDF studyExporter = new ExporterRDF();
            String exportPath = baseDir + "interactions.nq.gz";
            OutputStreamWriter writer = openStream(exportPath);
            int total = studies.size();
            int count = 1;
            for (StudyNode study : studies) {
                studyExporter.exportStudy(study, ExportUtil.AppenderWriter.of(writer, new ExportUtil.NQuadValueJoiner()), true);
                    if (count % 10000 == 0) {
                    LOG.info("added triples for [" + count + "] of [" + total + "] studies...");
                }
                count++;
            }
            LOG.info("adding triples for [" + total + "] of [" + total + "] studies.");

            LOG.info("writing nquads archive...");
            closeStream(exportPath, writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(List<StudyNode> studies, String pathPrefix, Map<String, DarwinCoreExporter> exporters) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(pathPrefix));
            FileWriter darwinCoreMeta = writeMetaHeader(pathPrefix);
            for (Map.Entry<String, DarwinCoreExporter> exporter : exporters.entrySet()) {
                export(studies, pathPrefix, exporter.getKey(), exporter.getValue(), darwinCoreMeta);
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

    private void export(List<StudyNode> importedStudies, String exportPath, String filename, DarwinCoreExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        OutputStreamWriter writer = openStream(exportPath + filename);
        for (StudyNode importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, ExportUtil.AppenderWriter.of(writer), includeHeader);
        }
        closeStream(exportPath + filename, writer);
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
