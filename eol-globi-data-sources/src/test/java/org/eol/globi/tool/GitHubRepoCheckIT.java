package org.eol.globi.tool;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.DatasetFinderException;
import org.junit.Test;

import java.io.IOException;

public class GitHubRepoCheckIT {

    @Test
    public void doSingleImportArgs() throws DatasetFinderException, StudyImporterException, IOException {
        final String repoName = "globalbioticinteractions/template-dataset";
        GitHubRepoCheck.main(new String[]{repoName});
    }

}