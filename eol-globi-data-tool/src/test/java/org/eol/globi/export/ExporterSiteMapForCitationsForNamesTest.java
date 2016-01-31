package org.eol.globi.export;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ExporterSiteMapForCitationsForNamesTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws NodeFactoryException, IOException {
        Study study = nodeFactory.getOrCreateStudy("title", "source", "citation 123");

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final StudyExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations(baseDirCitations);

        siteMapForCitationsExporter.exportStudy(study, null, true);

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=doi:citation%20123";
        assertSiteMap(baseDirCitations, substring);
    }

}