package org.trophic.graph.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.trophic.graph.data.*;
import org.trophic.graph.db.GraphService;

import java.util.Set;

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
        StudyImporter studyImporter = createStudyImporter(graphService);

        Set<String> studies = StudyLibrary.COLUMN_MAPPERS.keySet();
        for (String studyName : studies) {
            importStudy(studyImporter, studyName);
        }
    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService) {
        NodeFactory factory = new NodeFactory(graphService);
        return new StudyImporterImpl(new ParserFactoryImpl(), factory);
    }

    private static void importStudy(StudyImporter studyImporter, String studyName) throws StudyImporterException {
        System.out.println("study [" + studyName + "] importing ...");
        studyImporter.importStudy(studyName);
        System.out.println("study [" + studyName + "]");
    }

}