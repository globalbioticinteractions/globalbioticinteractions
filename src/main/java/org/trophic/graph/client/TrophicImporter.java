package org.trophic.graph.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.trophic.graph.data.*;

import java.util.Set;

public class TrophicImporter {

    private String storeDir = "data";

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().startImportStop(commandLineArguments);
    }

    public void startImportStop(String[] commandLineArguments) throws StudyImporterException {
        final GraphDatabaseService graphService = startNeo4j();
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

    private void importStudies(GraphDatabaseService graphService) throws StudyImporterException {
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

    protected GraphDatabaseService startNeo4j() {
        System.out.println("neo4j starting...");
        storeDir = "data";
        final GraphDatabaseService graphService = new EmbeddedGraphDatabase(storeDir);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("neo4j stopping...");
                graphService.shutdown();
                System.out.println("neo4j stopped.");
            }
        });
        System.out.println("neo4j started (" + ((EmbeddedGraphDatabase)graphService).getStoreDir() + ").");
        return graphService;
    }

    public void setStoreDir(String storeDir) {
        this.storeDir = storeDir;
    }
}
