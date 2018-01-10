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
    public void linkTaxonWithFunnyID() throws NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Donald duckus", "DUCK:123"));

        assertNotNull(taxon);

        new ImageLinker(getGraphDb(), System.out).link();

        Taxon enrichedTaxon = taxonIndex.findTaxonById("DUCK:123");
        assertThat(enrichedTaxon.getThumbnailUrl(), is(nullValue()));
        assertThat(enrichedTaxon.getExternalUrl(), is(nullValue()));
    }



}