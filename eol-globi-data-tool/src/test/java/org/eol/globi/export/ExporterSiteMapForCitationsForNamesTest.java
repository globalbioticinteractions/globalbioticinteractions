package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterSiteMapForCitationsForNamesTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws StudyImporterException, IOException {
        final Study study = nodeFactory.getOrCreateStudy("title", "source", "citation123");
        assertThat(study.getExternalId(), is("http://dx.doi.org/citation123"));

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final GraphExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations();

        siteMapForCitationsExporter.export(getGraphDb(), baseDirCitations.getAbsolutePath());

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=http://dx.doi.org/citation123";
        assertSiteMap(baseDirCitations, substring);
    }

}