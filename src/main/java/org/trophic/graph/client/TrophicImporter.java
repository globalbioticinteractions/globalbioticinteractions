package org.trophic.graph.client;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.*;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Study;
import org.trophic.graph.export.StudyExporter;
import org.trophic.graph.export.StudyExporterImpl;
import org.trophic.graph.export.StudyExporterPredatorPrey;
import org.trophic.graph.export.StudyExporterPredatorPreyEOL;
import org.trophic.graph.service.ExternalIdTaxonEnricher;
import org.trophic.graph.service.TaxonImageEnricher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrophicImporter {

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().startImportStop(commandLineArguments);
    }

    public void startImportStop(String[] commandLineArguments) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService();
        importTaxonony(graphService);

        List<Study> studies = importData(graphService);

        enrichData(graphService);

        exportData(studies);

        graphService.shutdown();
    }

    private void enrichData(GraphDatabaseService graphService) throws StudyImporterException {
        matchAgainstExternalTaxonomies(graphService);

        try {
            new TaxonImageEnricher(graphService).enrichTaxons();
        } catch (IOException e) {
            throw new StudyImporterException("failed to add image url information", e);
        }
    }

    private void matchAgainstExternalTaxonomies(GraphDatabaseService graphService) throws StudyImporterException {
        try {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            System.out.println("Matching taxons against external taxonomies starting...");
            new ExternalIdTaxonEnricher(graphService).enrichTaxons();
            stopwatch.stop();
            System.out.println("Matching taxons against external complete. Total duration: [" + stopwatch.getTime() / (60.0 * 1000.0) + "] minutes");
        } catch (IOException e) {
            throw new StudyImporterException("enriching unmatched nodes failed", e);
        }
    }

    private void importTaxonony(GraphDatabaseService graphService) throws StudyImporterException {
        OboImporter importer = new OboImporter(new NodeFactory(graphService));
        System.out.println("Taxonomy import starting...");
        importer.doImport();
        System.out.println("Taxonomy import complete.");
    }

    public List<Study> importStudies(GraphDatabaseService graphService) throws StudyImporterException {
        return importData(graphService);
    }

    private ArrayList<Study> importData(GraphDatabaseService graphService) throws StudyImporterException {
        ArrayList<StudyLibrary.Study> studies = new ArrayList<StudyLibrary.Study>();
        StudyLibrary.Study[] availableStudies = StudyLibrary.Study.values();
        studies.addAll(Arrays.asList(availableStudies));

        ArrayList<Study> importedStudies = new ArrayList<Study>();

        for (StudyLibrary.Study study : studies) {
            StudyImporter studyImporter = createStudyImporter(graphService, study);
            System.out.println("study [" + study + "] importing ...");
            importedStudies.add(studyImporter.importStudy());
            System.out.println("study [" + study + "]");
        }
        return importedStudies;
    }

    private void exportData(List<Study> importedStudies) throws StudyImporterException {
        try {
            export(importedStudies, "./export.csv", new StudyExporterImpl());
            export(importedStudies, "./exportPredatorTaxonPreyTaxon.csv", new StudyExporterPredatorPrey(GraphService.getGraphService()));
            export(importedStudies, "./exportPredatorTaxonPreyTaxonInteractionTypeEOL.csv", new StudyExporterPredatorPreyEOL(GraphService.getGraphService()));
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private void export(List<Study> importedStudies, String exportPath, StudyExporter studyExporter) throws IOException {
        FileWriter writer = new FileWriter(exportPath, false);
        System.out.println("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
        for (Study importedStudy : importedStudies) {
            boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
            studyExporter.exportStudy(importedStudy, writer, includeHeader);
        }
        writer.flush();
        writer.close();
        System.out.println("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService, StudyLibrary.Study study) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService);
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).createImporterForStudy(study);
    }

}