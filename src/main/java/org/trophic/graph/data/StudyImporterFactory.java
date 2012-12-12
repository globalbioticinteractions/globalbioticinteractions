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
        } else if (StudyLibrary.Study.MISSISSIPPI_ALABAMA.equals(study)) {
            importer = new StudyImporterForMississippiAlabama(parserFactory, nodeFactory);
        } else if (StudyLibrary.Study.LACAVA_BAY.equals(study)) {
            importer = new StudyImporterForLavacaBay(parserFactory, nodeFactory);
        } else if (StudyLibrary.Study.BLEWETT_CHARLOTTE_HARBOR_FL.equals(study)) {
            importer = new StudyImporterForSnook(parserFactory, nodeFactory);
        } else if (StudyLibrary.Study.BAREMORE_ANGEL_SHARK.equals(study)) {
            importer = new StudyImporterForBlaremore(parserFactory, nodeFactory);
        } else if (StudyLibrary.Study.BIO_INFO.equals(study)) {
            importer = new StudyImporterForBioInfo(parserFactory, nodeFactory);
        }
        return importer;
    }
}
