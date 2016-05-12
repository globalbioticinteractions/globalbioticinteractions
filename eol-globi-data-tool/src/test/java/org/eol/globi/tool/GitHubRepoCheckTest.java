package org.eol.globi.tool;

import org.apache.commons.cli.ParseException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.GitHubUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class GitHubRepoCheckTest {

    @Test
    public void doSingleImportArgs() throws ParseException, StudyImporterException, IOException, URISyntaxException {
        final String repoName = "globalbioticinteractions/template-dataset";
        GitHubRepoCheck.main(new String[]{repoName, GitHubUtil.getBaseUrlMaster(repoName)});
    }

}