package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ExporterSiteMapForNamesTest extends GraphDBNeo4jTestCase {

    @Test
    public void writeSiteMapWithNames() throws StudyImporterException, IOException {
        taxonIndex = new NonResolvingTaxonIndexNeo4j2(getGraphDb());
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("title", null, "citation 123"));
        TaxonImpl homoSapiens = new TaxonImpl("Homo sapiens", "homoSapiensId");
        homoSapiens.setPath("one two three");
        final Specimen human = nodeFactory.createSpecimen(study, homoSapiens);
        TaxonImpl dogTaxon = new TaxonImpl("Canis familiaris", null);
        final Specimen dog = nodeFactory.createSpecimen(study, dogTaxon);
        human.ate(dog);
        resolveNames();

        final File baseDirNames = createBaseDir("target/sitemap/names");

        final GraphExporter siteMapForNames = new ExporterSiteMapForNames();
        siteMapForNames.export(getGraphDb(), baseDirNames);
        assertSiteMap(baseDirNames, "http://www.globalbioticinteractions.org/?interactionType=interactsWith&sourceTaxon=Homo%20sapiens", "https://depot.globalbioticinteractions.org/snapshot/target/data/sitemap/names/sitemap.xml.gz");
    }

    public void assertSiteMap(File baseDirCitations, String substring, String siteMapLocation) throws IOException {
        final File file = new File(baseDirCitations, "sitemap.xml.gz");
        assertThat(file.exists(), is(true));
        final String siteMapString = IOUtils.toString(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8);
        assertThat(siteMapString,
                containsString(StringEscapeUtils.escapeXml(substring)));
        final File sitemapIndex = new File(baseDirCitations, "sitemap_index.xml");
        assertThat(sitemapIndex.exists(), is(true));
        final String sitemapIndexString = IOUtils.toString(new FileInputStream(sitemapIndex), StandardCharsets.UTF_8);
        assertThat(sitemapIndexString, containsString(siteMapLocation));
    }

    public File createBaseDir(String pathname) throws IOException {
        final File baseDirCitations = new File(pathname);
        FileUtils.deleteQuietly(baseDirCitations);
        FileUtils.forceMkdir(baseDirCitations);
        assertThat(new File(baseDirCitations, "sitemap.xml").exists(), is(false));
        return baseDirCitations;
    }

}