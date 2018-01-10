package org.eol.globi.data;


import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class StudyImporterForGeminaIT extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudyGitHubMostRecent() throws StudyImporterException, IOException, URISyntaxException {
        StudyImporterForGemina importer = new StudyImporterForGemina(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);
    }


}