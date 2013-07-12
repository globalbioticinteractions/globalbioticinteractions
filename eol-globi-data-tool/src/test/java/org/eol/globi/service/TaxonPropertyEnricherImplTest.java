package org.eol.globi.service;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TaxonPropertyEnricherImplTest extends GraphDBTestCase {

    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        TaxonPropertyEnricherImpl enricher = new TaxonPropertyEnricherImpl(getGraphDb());

        Taxon taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        enricher.enrich(taxon);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), is(not("no:match")));


        taxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        enricher.enrich(taxon);
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), is(not("no:match")));

        Taxon sameTaxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = nodeFactory.getOrCreateTaxon("Pitar fulminatus");
        enricher.enrich(taxon);
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not("no:match")));

        assertThat(enricher.enrich(taxon), is(false));
    }
}
