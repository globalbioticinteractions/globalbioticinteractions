package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ImageLinkerTest extends GraphDBTestCase {

    @Test
    public void linkSingleImage() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon(new TaxonImpl("Homo sapiens", "EOL:327955"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb());

        TaxonNode enrichedTaxon = nodeFactory.findTaxonById("EOL:327955");
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("thumbnailUrl"), is("http://media.eol.org/content/2014/08/07/23/02836_98_68.jpg"));
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("externalUrl"), is("http://eol.org/pages/327955"));
        assertThat((String)enrichedTaxon.getUnderlyingNode().getProperty("imageUrl"), is("http://media.eol.org/content/2014/08/07/23/02836_orig.jpg"));
    }

    @Test
    public void linkTaxonWithFunnyID() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon(new TaxonImpl("Donald duckus", "DUCK:123"));

        assertNotNull(taxon);

        new ImageLinker().linkImages(getGraphDb());

        TaxonNode enrichedTaxon = nodeFactory.findTaxonById("DUCK:123");
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("thumbnailUrl"), is(false));
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("externalUrl"), is(false));
        assertThat(enrichedTaxon.getUnderlyingNode().hasProperty("imageUrl"), is(false));
    }



}