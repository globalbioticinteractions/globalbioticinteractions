package org.eol.globi.tool;

import org.apache.commons.cli.ParseException;
import org.eol.globi.data.StudyImporterException;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class GitHubRepoCheckTest {

    @Test
    public void doSingleImportArgs() throws ParseException, StudyImporterException, IOException, URISyntaxException {
        GitHubRepoCheck.main(new String[]{"globalbioticinteractions/template-dataset"});
    }

}