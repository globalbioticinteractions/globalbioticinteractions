package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonPropertyEnricherImplIT extends GraphDBTestCase {

    private TaxonPropertyEnricher enricher;

    @Before
    public void start() {
        enricher = TaxonPropertyEnricherFactory.createTaxonEnricher(getGraphDb());
        nodeFactory = new NodeFactory(getGraphDb(), enricher);
    }


    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        Taxon sameTaxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = nodeFactory.getOrCreateTaxon("Pitar fulminatus");
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Foraminifera");
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }

    @Test
    public void chromatomyiaScabiosae() throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Chromatomyia scabiosae");
        assertThat(taxon.getName(), is("Chromatomyia scabiosae"));
        assertThat(taxon.getPath(), is(""));
        assertThat(taxon.getExternalId(), is(nullValue()));
    }

    @Test
    public void otherSuspensionFeeders() throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Other suspension feeders");
        assertThat(taxon.getName(), is("Other suspension feeders"));
        assertThat(taxon.getPath(), is(""));
        assertThat(taxon.getExternalId(), is(nullValue()));
    }

    @Test
    public void sediment() throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Sediment");
        assertThat(taxon.getName(), is("Sediment"));
        assertThat(taxon.getPath(), is(""));
        assertThat(taxon.getExternalId(), is(nullValue()));
    }
}
