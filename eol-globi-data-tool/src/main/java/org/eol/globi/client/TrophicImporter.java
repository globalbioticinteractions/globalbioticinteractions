package org.eol.globi.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.export.InteractionsExporter;
import org.eol.globi.export.StudyExportUnmatchedTaxaForStudies;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TaxonPropertyEnricherImpl;
import org.neo4j.graphdb.GraphDatabaseService;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.ParserFactory;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterFactory;
import org.eol.globi.data.StudyLibrary;
import org.eol.globi.db.GraphService;
import org.eol.globi.domain.Study;
import org.eol.globi.export.StudyExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrophicImporter {
    private static final Log LOG = LogFactory.getLog(TrophicImporter.class);

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().importExport();
    }

    public void importExport() throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService();
        TaxonPropertyEnricherImpl taxonEnricher = new TaxonPropertyEnricherImpl(graphService);
        List<Study> studies = importData(graphService, taxonEnricher);
        exportData(studies);
        graphService.shutdown();
    }


    public List<Study> importStudies(GraphDatabaseService graphService, TaxonPropertyEnricher taxonEnricher) throws StudyImporterException {
        return importData(graphService, taxonEnricher);
    }

    private ArrayList<Study> importData(GraphDatabaseService graphService, TaxonPropertyEnricher taxonEnricher) throws StudyImporterException {
        ArrayList<StudyLibrary.Study> studies = new ArrayList<StudyLibrary.Study>();
        StudyLibrary.Study[] availableStudies = StudyLibrary.Study.values();
        studies.addAll(Arrays.asList(availableStudies));

        ArrayList<Study> importedStudies = new ArrayList<Study>();

        for (StudyLibrary.Study study : studies) {
            StudyImporter studyImporter = createStudyImporter(graphService, study, taxonEnricher);
            LOG.info("study [" + study + "] importing ...");
            importedStudies.add(studyImporter.importStudy());
            LOG.info("study [" + study + "] imported.");
        }
        return importedStudies;
    }

    private void exportData(List<Study> importedStudies) throws StudyImporterException {
        try {
            FileWriter darwinCoreMeta = writeMetaHeader();
            export(importedStudies, "./unmatchedSourceTaxa.csv", new StudyExportUnmatchedTaxaForStudies(GraphService.getGraphService()), darwinCoreMeta);
            export(importedStudies, "./interactions.csv", new InteractionsExporter(), darwinCoreMeta);
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

    private FileWriter writeMetaHeader() throws IOException {
        FileWriter darwinCoreMeta = new FileWriter("./meta.xml", false);
        darwinCoreMeta.write("<?xml version=\"1.0\"?>\n" +
                "<archive xmlns=\"http://rs.tdwg.org/dwc/text/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://rs.tdwg.org/dwc/text/  http://services.eol.org/schema/dwca/tdwg_dwc_text.xsd\">\n");
        return darwinCoreMeta;
    }

    private void export(List<Study> importedStudies, String exportPath, StudyExporter studyExporter, FileWriter darwinCoreMeta) throws IOException {
        FileWriter writer = new FileWriter(exportPath, false);
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
        }
        studyExporter.exportDarwinCoreMetaTable(darwinCoreMeta, exportPath);
        writer.flush();
        writer.close();
        LOG.info("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService, StudyLibrary.Study study, TaxonPropertyEnricher taxonEnricher) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService, taxonEnricher);
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).createImporterForStudy(study);
    }

}