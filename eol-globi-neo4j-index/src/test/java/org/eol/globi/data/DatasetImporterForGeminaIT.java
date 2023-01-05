package org.eol.globi.data;


import org.junit.Test;

public class DatasetImporterForGeminaIT extends GraphDBNeo4jTestCase {

    @Test
    public void createAndPopulateStudyGitHubMostRecent() throws StudyImporterException {
        DatasetImporterForGemina importer = new DatasetImporterForGemina(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        importStudy(importer);
    }


}