package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class NameResolverTest extends GraphDBTestCase {

    @Test
    public void doNameResolving() throws NodeFactoryException, PropertyEnricherException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Homo sapiens", null));
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Canis lupus", "EOL:1"));
        human.ate(animal);
        Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Arius felis", null));
        human.ate(fish);

        assertNull(taxonIndex.findTaxonById("EOL:1"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        final NameResolver nameResolver = new NameResolver(getGraphDb());
        nameResolver.setBatchSize(1L);
        nameResolver.resolve();

        assertAnimalia(taxonIndex.findTaxonById("EOL:1"));

        assertThat(taxonIndex.findTaxonByName("Arius felis"), is(notNullValue()));
        assertThat(taxonIndex.findTaxonByName("Ariopsis felis"), is(notNullValue()));

        TaxonNode homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);
        assertThat(homoSapiens.getExternalId(), is("EOL:327955"));
    }

    public void assertAnimalia(TaxonNode animalia) {
        assertNotNull(animalia);
        assertThat(animalia.getName(), is("Animalia"));
        assertThat(animalia.getPath(), is("Animalia"));
    }

    @Test
    public void iNaturalistTaxon() throws NodeFactoryException {
        Specimen someOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy("bla1"), new TaxonImpl("Heterotheca grandiflora", null));
        Specimen someOtherOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Blaus bla", "INAT_TAXON:58831"));
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(nodeFactory.createStudy("bla"), new TaxonImpl("Redus rha", "INAT_TAXON:126777"));
        someOtherOrganism.ate(someOtherOrganism2);

        final NameResolver nameResolver = new NameResolver(getGraphDb());
        nameResolver.setBatchSize(1L);
        nameResolver.resolve();

        TaxonNode resolvedTaxon = taxonIndex.findTaxonById("INAT_TAXON:58831");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(resolvedTaxon.getName(), is("Heterotheca grandiflora"));
        TaxonNode resolvedTaxon2 = taxonIndex.findTaxonByName("Heterotheca grandiflora");
        assertThat(resolvedTaxon2, is(notNullValue()));
        assertThat(resolvedTaxon2.getExternalId(), is("EOL:578180"));
    }

    @Test
    public void progressMessage() {
        assertThat(NameResolver.getProgressMsg(10000L, 5555), is("[1800.18] taxon/s over [5.56] s"));
    }

}
