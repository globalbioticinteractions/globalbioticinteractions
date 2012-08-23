package org.trophic.graph.client;

import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.*;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Study;
import org.trophic.graph.service.TaxonEnricher;

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
        List<Study> studies = importStudies(graphService);
        exportData(studies);

        try {
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            System.out.println("Matching taxons against WoRMS starting...");
            new TaxonEnricher(graphService).enrichTaxons();
            stopwatch.stop();
            System.out.println("Matching taxons against WoRMS complete. Total duration: [" + stopwatch.getTime()/(60.0*1000.0) + "] minutes");
        } catch (IOException e) {
            throw new StudyImporterException("enriching unmatched nodes failed", e);
        }

        graphService.shutdown();
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
            String exportPath = "./export.csv";
            FileWriter writer = new FileWriter(exportPath, false);
            System.out.println("export data to [" + new File(exportPath).getAbsolutePath() + "] started...");
            for (Study importedStudy : importedStudies) {
                boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
                new StudyExporterImpl().exportStudy(importedStudy, writer, includeHeader);
            }
            writer.flush();
            writer.close();
            System.out.println("export data to [" + new File(exportPath).getAbsolutePath() + "] complete.");
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }
    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService, StudyLibrary.Study study) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService);
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).createImporterForStudy(study);
    }

}