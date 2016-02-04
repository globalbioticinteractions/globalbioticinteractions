package org.eol.globi.export;

import org.apache.commons.lang.StringEscapeUtils;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterSiteMapForCitationsTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws StudyImporterException, IOException {
        final Study study = nodeFactory.getOrCreateStudy("title", "source", "citation123&bla");
        assertThat(study.getExternalId(), is("http://dx.doi.org/citation123&bla"));

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final GraphExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations();

        siteMapForCitationsExporter.export(getGraphDb(), baseDirCitations.getAbsolutePath());

        final String escapedString = StringEscapeUtils.escapeXml("http://dx.doi.org/citation123&bla");

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=" + escapedString;
        assertSiteMap(baseDirCitations, substring, "https://globi.s3.amazonaws.com/snapshot/target/data/sitemap/citations/sitemap.xml.gz");
    }

}