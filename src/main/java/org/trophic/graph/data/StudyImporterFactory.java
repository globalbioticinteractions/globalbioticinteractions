package org.trophic.graph.data;

public class StudyImporterFactory {
    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter createImporterForStudy(StudyLibrary.Study study) {
        StudyImporter importer = null;
        if (StudyLibrary.Study.AKIN_MAD_ISLAND.equals(study)) {
            importer = new StudyImporterForAkin(parserFactory, nodeFactory);
        } else if (StudyLibrary.Study.LACAVA_BAY.equals(study) || StudyLibrary.Study.MISSISSIPPI_ALABAMA.equals(study)) {
            importer = new StudyImporterImpl(parserFactory, nodeFactory);
        }
        return importer;
    }
}
