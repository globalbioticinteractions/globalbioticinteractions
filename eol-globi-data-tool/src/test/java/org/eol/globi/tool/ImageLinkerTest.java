package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class ImageLinkerTest extends GraphDBTestCase {

    @Test
    public void linkSingleImage() throws NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", "EOL:327955"));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Whatevero whateverens", "EOL:8888888888"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb(), System.out);

        Taxon enrichedTaxon = taxonIndex.findTaxonById("EOL:327955");
        assertThat(enrichedTaxon.getThumbnailUrl(), containsString("http://media.eol.org/content/"));
        assertThat(enrichedTaxon.getExternalUrl(), is("http://eol.org/pages/327955"));
    }

    @Test
    public void linkTaxonWithFunnyID() throws NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Donald duckus", "DUCK:123"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb(), System.out);

        Taxon enrichedTaxon = taxonIndex.findTaxonById("DUCK:123");
        assertThat(enrichedTaxon.getThumbnailUrl(), is(nullValue()));
        assertThat(enrichedTaxon.getExternalUrl(), is(nullValue()));
    }



}