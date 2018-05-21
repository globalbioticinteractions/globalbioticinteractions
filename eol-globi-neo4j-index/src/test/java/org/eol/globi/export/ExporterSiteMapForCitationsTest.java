package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExporterSiteMapForCitationsTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws StudyImporterException, IOException {
        final Study study = nodeFactory.getOrCreateStudy(new StudyImpl("title", "source", "doi:some/doi", "citation123&bla"));
        assertThat(study.getExternalId(), is("https://doi.org/some/doi"));

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final GraphExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations();

        siteMapForCitationsExporter.export(getGraphDb(), baseDirCitations.getAbsolutePath());

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=" + "https://doi.org/some/doi";
        assertSiteMap(baseDirCitations, substring, "https://depot.globalbioticinteractions.org/snapshot/target/data/sitemap/citations/sitemap.xml.gz");
    }

}