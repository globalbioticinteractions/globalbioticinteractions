package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ExporterSiteMapForCitationsForNamesTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws StudyImporterException, IOException {
        nodeFactory.getOrCreateStudy("title", "source", "citation 123");

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final GraphExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations();

        siteMapForCitationsExporter.export(getGraphDb(), baseDirCitations.getAbsolutePath());

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=doi:citation%20123";
        assertSiteMap(baseDirCitations, substring);
    }

}