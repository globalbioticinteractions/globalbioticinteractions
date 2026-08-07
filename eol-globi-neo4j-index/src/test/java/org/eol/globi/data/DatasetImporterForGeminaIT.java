package org.eol.globi.data;


import org.junit.Test;

public class DatasetImporterForGeminaIT extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudyGitHubMostRecent() throws StudyImporterException {
        DatasetImporterForGemina importer = new DatasetImporterForGemina(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        importStudy(importer);
    }


}