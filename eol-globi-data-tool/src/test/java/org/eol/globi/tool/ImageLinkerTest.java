package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class ImageLinkerTest extends GraphDBTestCase {

    @Test
    public void linkSingleImage() throws NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", "EOL:327955"));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Whatevero whateverens", "EOL:8888888888"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb(), System.out);

        TaxonNode enrichedTaxon = taxonIndex.findTaxonById("EOL:327955");
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("thumbnailUrl"), containsString("http://media.eol.org/content/"));
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("externalUrl"), is("http://eol.org/pages/327955"));
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("imageUrl"), containsString("http://media.eol.org/content/"));
    }

    @Test
    public void linkTaxonWithFunnyID() throws NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Donald duckus", "DUCK:123"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb(), System.out);

        TaxonNode enrichedTaxon = taxonIndex.findTaxonById("DUCK:123");
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("thumbnailUrl"), is(false));
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("externalUrl"), is(false));
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("imageUrl"), is(false));
    }



}