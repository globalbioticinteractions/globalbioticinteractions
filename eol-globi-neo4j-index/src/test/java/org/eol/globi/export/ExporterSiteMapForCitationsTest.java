package org.eol.globi.export;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.globalbioticinteractions.doi.DOI;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class ExporterSiteMapForCitationsTest extends ExporterSiteMapForNamesTest {

    @Test
    public void writeSiteMapWithCitations() throws StudyImporterException, IOException {
        final Study study = nodeFactory.getOrCreateStudy(new StudyImpl("title", new DOI("some", "doi"), "citation123&bla"));
        assertThat(study.getExternalId(), is("https://doi.org/10.some/doi"));

        final File baseDirCitations = createBaseDir("target/sitemap/citations");

        final GraphExporter siteMapForCitationsExporter = new ExporterSiteMapForCitations();

        siteMapForCitationsExporter.export(getGraphDb(), baseDirCitations.getAbsolutePath());

        final String substring = "http://www.globalbioticinteractions.org/?accordingTo=" + "https://doi.org/10.some/doi";
        assertSiteMap(baseDirCitations, substring, "https://depot.globalbioticinteractions.org/snapshot/target/data/sitemap/citations/sitemap.xml.gz");
    }

}