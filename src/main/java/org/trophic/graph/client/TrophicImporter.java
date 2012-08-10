package org.trophic.graph.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.trophic.graph.data.*;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Study;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

public class TrophicImporter {

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().startImportStop(commandLineArguments);
    }

    public void startImportStop(String[] commandLineArguments) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService();
        importStudies(graphService);
        int count = 0;
        for (Node node : graphService.getAllNodes()) {
            System.out.print(count + ":{");
            for (String key : node.getPropertyKeys()) {
                System.out.println(key + "=" + node.getProperty(key));
            }
            System.out.println("}");
            count++;
        }
        graphService.shutdown();
    }

    public void importStudies(GraphDatabaseService graphService) throws StudyImporterException {
        ArrayList<StudyLibrary.Study> studies = new ArrayList<StudyLibrary.Study>();
        studies.add(StudyLibrary.Study.AKIN_MAD_ISLAND);
        studies.add(StudyLibrary.Study.LACAVA_BAY);
        studies.add(StudyLibrary.Study.MISSISSIPPI_ALABAMA);

        ArrayList<Study> importedStudies = new ArrayList<Study>();

        for (StudyLibrary.Study study : studies) {
            StudyImporter studyImporter = createStudyImporter(graphService, study);
            System.out.println("study [" + study + "] importing ...");
            importedStudies.add(studyImporter.importStudy());
            System.out.println("study [" + study + "]");
        }

        try {
            Writer writer = new FileWriter("./export.csv", false);
            for (Study importedStudy : importedStudies) {
                new StudyExporterImpl().exportStudy(importedStudy, writer);
            }
            writer.flush();
            writer.close();
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