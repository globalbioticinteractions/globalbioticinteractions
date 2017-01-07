package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.DatasetFinderException;
import org.junit.Test;

import java.io.IOException;

public class GitHubRepoCheckTest {

    @Test
    public void doSingleImportArgs() throws DatasetFinderException, StudyImporterException, IOException {
        GitHubRepoCheck.main(new String[]{"globalbioticinteractions/template-dataset"});
    }

    @Test
    public void inaturalist() throws DatasetFinderException, StudyImporterException, IOException {
        GitHubRepoCheck.main(new String[]{"globalbioticinteractions/inaturalist"});
    }

}