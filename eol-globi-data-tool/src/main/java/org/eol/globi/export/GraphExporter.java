package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class GraphExporter {
    private static final Log LOG = LogFactory.getLog(GraphExporter.class);

    public void export(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
        } catch (IOException e) {
            throw new StudyImporterException("failed to create output dir [" + baseDir + "]", e);
        }
        List<Study> studies = NodeFactory.findAllStudies(graphService);
        exportDataOntology(studies, baseDir);
        exportUnmatchedTaxa(studies, baseDir);
        exportGoMexSI(studies, baseDir);
        exportDarwinCoreAggregatedByStudy(baseDir, studies);
        exportDarwinCoreAll(baseDir, studies);
    }

    private void exportUnmatchedTaxa(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            OutputStreamWriter writer1 = openStream(baseDir + "unmatchedSourceTaxa.csv");
            for (Study importedStudy1 : studies) {
                boolean includeHeader1 = studies.indexOf(importedStudy1) == 0;
                new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(importedStudy1, writer1, includeHeader1);
            }
            closeStream(baseDir + "unmatchedSourceTaxa.csv", writer1);
            OutputStreamWriter writer = openStream(baseDir + "unmatchedTargetTaxa.csv");
            for (Study importedStudy : studies) {
                boolean includeHeader = studies.indexOf(importedStudy) == 0;
                new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(importedStudy, writer, includeHeader);
            }
            closeStream(baseDir + "unmatchedTargetTaxa.csv", writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export unmatched source taxa", e);
        }
    }

    // Provide simple data representation
    private void exportGoMexSI(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
            OutputStreamWriter writer = openStream(baseDir + "GoMexSIInteractionsTaxa.csv");
            for (Study importedStudy : studies) {
                boolean includeHeader = studies.indexOf(importedStudy) == 0;
                new ExporterGoMexSI().exportStudy(importedStudy, writer, includeHeader);
            }
            closeStream(baseDir + "GoMexSIInteractionsTaxa.csv", writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export GoMexSI", e);
        }
    }

    private void exportDarwinCoreAggregatedByStudy(String baseDir, List<Study> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies,
                baseDir + "aggregatedByStudy/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.csv", new ExporterAssociationAggregates());
                put("occurrence.csv", new ExporterOccurrenceAggregates());
                put("references.csv", new ExporterReferences());
                put("taxa.csv", new ExporterTaxa());
            }
        });
    }

    private void exportDarwinCoreAll(String baseDir, List<Study> studies) throws StudyImporterException {
        exportDarwinCoreArchive(studies, baseDir + "all/", new HashMap<String, DarwinCoreExporter>() {
            {
                put("association.csv", new ExporterAssociations());
                put("occurrence.csv", new ExporterOccurrences());
                put("references.csv", new ExporterReferences());
                put("taxa.csv", new ExporterTaxa());
                put("measurementOrFact.csv", new ExporterMeasurementOrFact());
            }
        });
    }

    private void exportDataOntology(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            // only exporting SPIRE studies to ttl for purposes of testing the globi ontology
            List<Study> spireStudies = new ArrayList<Study>();
            for (Study study : studies) {
                if (study.getSource().contains(StudyImporterForSPIRE.SOURCE_SPIRE)) {
                    spireStudies.add(study);
                }
            }
            LittleTurtleExporter studyExporter = new LittleTurtleExporter();
            OutputStreamWriter writer = openStream(baseDir + "globi.ttl.gz");
            for (Study importedStudy : spireStudies) {
                studyExporter.exportStudy(importedStudy, writer, true);
            }
            studyExporter.exportDataOntology(writer);
            closeStream(baseDir + "globi.ttl.gz", writer);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(List<Study> studies, String pathPrefix, Map<String, DarwinCoreExporter> exporters) throws StudyImporterException {
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

    private void export(List<Study> importedStudies, String exportPath, String filename, DarwinCoreExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        OutputStreamWriter writer = openStream(exportPath + filename);
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
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
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        return writer;
    }

}
