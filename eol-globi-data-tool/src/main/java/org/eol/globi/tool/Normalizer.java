package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.export.DarwinCoreExporter;
import org.eol.globi.export.ExporterAssociationAggregates;
import org.eol.globi.export.ExporterAssociations;
import org.eol.globi.export.ExporterAssociationsBase;
import org.eol.globi.export.ExporterMeasurementOrFact;
import org.eol.globi.export.ExporterOccurrenceAggregates;
import org.eol.globi.export.ExporterOccurrences;
import org.eol.globi.export.ExporterOccurrencesBase;
import org.eol.globi.export.ExporterReferences;
import org.eol.globi.export.ExporterTaxa;
import org.eol.globi.export.GlobiOWLExporter;
import org.eol.globi.export.StudyExportUnmatchedSourceTaxaForStudies;
import org.eol.globi.export.StudyExportUnmatchedTargetTaxaForStudies;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.Study;
import org.eol.globi.export.StudyExporter;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Normalizer {
    private static final Log LOG = LogFactory.getLog(Normalizer.class);

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new Normalizer().normalize();
    }

    public void normalize() throws StudyImporterException {
        normalize("./");
    }

    public void normalize(String baseDir) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService(baseDir);
        importData(graphService, TaxonPropertyEnricherFactory.createTaxonEnricher(graphService));
        exportData(graphService, baseDir);
        graphService.shutdown();
    }


    protected void exportData(GraphDatabaseService graphService, String baseDir) throws StudyImporterException {
        List<Study> studies = NodeFactory.findAllStudies(graphService);
        exportDarwinCoreArchive(studies,
                new ExporterAssociationAggregates(),
                new ExporterOccurrenceAggregates(),
                baseDir + "aggregatedByStudy/");
        exportDarwinCoreArchive(studies,
                new ExporterAssociations(),
                new ExporterOccurrences(),
                baseDir + "all/");
        exportDataOntology(studies, baseDir);
    }

    private void exportDataOntology(List<Study> studies, String baseDir) throws StudyImporterException {
        try {
            export(studies, baseDir + "globi.ttl.gz", new GlobiOWLExporter());
        } catch (OWLOntologyCreationException e) {
            throw new StudyImporterException("failed to export as owl", e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export as owl", e);
        }
    }

    private void exportDarwinCoreArchive(List<Study> studies, ExporterAssociationsBase associationExporter, ExporterOccurrencesBase occurrenceExporter, String pathPrefix) throws StudyImporterException {
        try {
            FileUtils.forceMkdir(new File(pathPrefix));
            FileWriter darwinCoreMeta = writeMetaHeader(pathPrefix);
            export(studies, pathPrefix, "unmatchedSourceTaxa.csv", new StudyExportUnmatchedSourceTaxaForStudies(), darwinCoreMeta);
            export(studies, pathPrefix, "unmatchedTargetTaxa.csv", new StudyExportUnmatchedTargetTaxaForStudies(), darwinCoreMeta);
            export(studies, pathPrefix, "association.csv", associationExporter, darwinCoreMeta);
            export(studies, pathPrefix, "occurrence.csv", occurrenceExporter, darwinCoreMeta);
            export(studies, pathPrefix, "references.csv", new ExporterReferences(), darwinCoreMeta);
            export(studies, pathPrefix, "taxa.csv", new ExporterTaxa(), darwinCoreMeta);
            export(studies, pathPrefix, "measurementOrFact.csv", new ExporterMeasurementOrFact(), darwinCoreMeta);
            writeMetaFooter(darwinCoreMeta);
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private void importData(GraphDatabaseService graphService, TaxonPropertyEnricher taxonEnricher)  {
        for (Class importer : StudyImporterFactory.getAvailableImporters()) {
            try {
                importData(graphService, taxonEnricher, importer);
            } catch (StudyImporterException e) {
                LOG.error("problem encountered while importing [" + importer.getName() + "]", e);
            }
        }
    }

    protected void importData(GraphDatabaseService graphService, TaxonPropertyEnricher taxonEnricher, Class importer) throws StudyImporterException {
        StudyImporter studyImporter = createStudyImporter(graphService, importer, taxonEnricher);
        LOG.info("[" + importer + "] importing ...");
        studyImporter.importStudy();
        LOG.info("[" + importer + "] imported.");
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


    private StudyImporter createStudyImporter(GraphDatabaseService graphService, Class<StudyImporter> studyImporter, TaxonPropertyEnricher taxonEnricher) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService, taxonEnricher);
        factory.setDoiResolver(new DOIResolverImpl());
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).instantiateImporter(studyImporter);
    }

}