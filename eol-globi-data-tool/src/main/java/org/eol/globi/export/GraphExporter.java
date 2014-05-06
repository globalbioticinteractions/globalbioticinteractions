package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.GraphDatabaseService;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

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
        List<Study> studies = NodeFactory.findAllStudies(graphService);
        exportUnmatchedTaxa(studies, baseDir);
        exportGoMexSI(studies, baseDir);
        exportDarwinCoreAggregatedByStudy(baseDir, studies);
        exportDarwinCoreAll(baseDir, studies);
        exportDataOntology(studies, baseDir);
    }

    private void exportUnmatchedTaxa(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
            export(studies, baseDir + "unmatchedSourceTaxa.csv", new StudyExportUnmatchedSourceTaxaForStudies());
            export(studies, baseDir + "unmatchedTargetTaxa.csv", new StudyExportUnmatchedTargetTaxaForStudies());
        } catch (IOException e) {
            throw new StudyImporterException("failed to export unmatched source taxa", e);
        }
    }

    // Provide simple data representation
    private void exportGoMexSI(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(baseDir));
            export(studies, baseDir + "interactionsGoMexSI.csv", new ExporterGoMexSI());
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
            export(spireStudies, baseDir + "globi.ttl.gz", new GlobiOWLExporter());
        } catch (OWLOntologyCreationException e) {
            throw new StudyImporterException("failed to export as owl", e);
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
        export(importedStudies, exportPath + filename, studyExporter);
        LOG.info("darwin core meta file writing... ");
        studyExporter.exportDarwinCoreMetaTable(darwinCoreMeta, filename);
        LOG.info("darwin core meta file written. ");
    }

    private void export(List<Study> importedStudies, String exportPath, StudyExporter studyExporter) throws IOException {
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(exportPath));
        if (exportPath.endsWith(".gz")) {
            fos = new GZIPOutputStream(fos);
        }
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
        }
        writer.flush();
        writer.close();
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }

}
