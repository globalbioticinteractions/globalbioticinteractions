package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.StringContains.containsString;

public class ExporterSiteMapForNamesTest extends GraphDBTestCase {

    @Test
    public void writeSiteMapWithNames() throws NodeFactoryException, IOException {
        final PropertyEnricher taxonEnricher = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) {
                Taxon taxon = new TaxonImpl();
                TaxonUtil.mapToTaxon(properties, taxon);
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four five six");
                }
                return TaxonUtil.taxonToMap(taxon);
            }

            @Override
            public void shutdown() {

            }
        };
        taxonIndex = ExportTestUtil.taxonIndexWithEnricher(taxonEnricher, getGraphDb());
        Study study = nodeFactory.getOrCreateStudy("title", "source", "citation 123");
        final Specimen human = nodeFactory.createSpecimen(study, "Homo sapiens");
        final Specimen dog = nodeFactory.createSpecimen(study, "Canis familiaris");
        human.ate(dog);
        resolveNames();

        final File baseDirNames = createBaseDir("target/sitemap/names");

        final StudyExporter siteMapForNames = new ExporterSiteMapForNames(baseDirNames);

        siteMapForNames.exportStudy(study, null, true);
        assertSiteMap(baseDirNames, "http://www.globalbioticinteractions.org/?interactionType=interactsWith&sourceTaxonName=Homo%20sapiens");
    }

    public void assertSiteMap(File baseDirCitations, String substring) throws IOException {
        final File file = new File(baseDirCitations, "sitemap.xml");
        assertThat(file.exists(), is(true));
        final String siteMapString = IOUtils.toString(new FileInputStream(file));
        assertThat(siteMapString,
                containsString(substring));
        assertThat(new File(baseDirCitations, "sitemap_index.xml").exists(), is(true));
    }

    public File createBaseDir(String pathname) throws IOException {
        final File baseDirCitations = new File(pathname);
        FileUtils.deleteQuietly(baseDirCitations);
        FileUtils.forceMkdir(baseDirCitations);
        assertThat(new File(baseDirCitations, "sitemap.xml").exists(), is(false));
        return baseDirCitations;
    }

}