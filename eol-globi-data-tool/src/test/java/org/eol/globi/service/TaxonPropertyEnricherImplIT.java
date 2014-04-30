package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.TaxonNode;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonPropertyEnricherImplIT extends GraphDBTestCase {

    private TaxonPropertyEnricher enricher;

    @Before
    public void start() {
        enricher = TaxonPropertyEnricherFactory.createTaxonEnricher();
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonServiceImpl(enricher, new TaxonNameCorrector(), getGraphDb()));
    }


    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Homo sapiens", "blabla", null);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getRank(), containsString("Species"));

        taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        TaxonNode sameTaxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = nodeFactory.getOrCreateTaxon("Pitar fulminatus");
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Foraminifera");
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }


    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/59
    public void greySmoothhound() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Grey Smoothhound");
        assertThat(taxon.getExternalId(), is("EOL:207918"));
        assertThat(taxon.getName(), is("Mustelus californicus"));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("gall tissue (Q. robur)");
        assertThat(taxon.getName(), is("Quercus robur"));
        assertThat(taxon.getExternalId(), is("EOL:1151323"));
    }

    @Test
    public void chromatomyiaScabiosae() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Chromatomyia scabiosae");
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void otherSuspensionFeeders() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Other suspension feeders");
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void sediment() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Sediment");
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void noNameButExternalId() throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen("no name", "EOL:223038");
        assertThat(specimen, is(notNullValue()));
        Iterable<Relationship> classifications = specimen.getClassifications();
        int count = 0;
        for (Relationship classification : classifications) {
            TaxonNode taxonNode = new TaxonNode(classification.getEndNode());
            assertThat(taxonNode.getName(), is("Ariopsis felis"));
            assertThat(taxonNode.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis | no name"));
            assertThat(taxonNode.getExternalId(), is("EOL:223038"));
            count++;
        }
        assertThat(count, is(1));
    }

}
