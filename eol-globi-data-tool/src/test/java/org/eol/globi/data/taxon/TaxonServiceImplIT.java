package org.eol.globi.data.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TaxonServiceImplIT extends GraphDBTestCase {

    private TaxonServiceImpl taxonService;
    private static TaxonPropertyEnricher taxonEnricher = null;

    @BeforeClass
    public static void initEnricher() {
        taxonEnricher = TaxonPropertyEnricherFactory.createTaxonEnricher();
    }

    @Before
    public void init() {
        this.taxonService = new TaxonServiceImpl(taxonEnricher, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb());
    }

    @Test
    public void createTaxonFish() throws NodeFactoryException {
        taxonService.setCorrector(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return StringUtils.equals("Fish", taxonName) ? "Actinopterygii" : taxonName;
            }
        });

        TaxonNode taxon = taxonService.getOrCreateTaxon("Fish", null, null);
        assertThat(taxon.getName(), is("Actinopterygii"));
        taxon = taxonService.getOrCreateTaxon("Fish", null, null);
        assertThat(taxon.getName(), is("Actinopterygii"));

        assertZeroHits(taxonService, "fish");
    }

    @Test
    public void createNoMatch() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        TaxonNode taxon = taxonService.getOrCreateTaxon("Santa Claus meets Superman", null, null);
        assertThat(taxon.getName(), is("Santa Claus meets Superman"));
        assertThat(taxon.getExternalId(), is("no:match"));
        assertThat(taxon.getPath(), is(nullValue()));
        assertThat(taxon.getCommonNames(), is(nullValue()));

        assertZeroHits(taxonService, "no:match");
    }

    @Test
    public void noDuplicatesOnSynomyms() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Galeichthys felis", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Ariopsis felis", null, null);
        TaxonNode third = taxonService.getOrCreateTaxon("Arius felis", null, null);
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(third.getNodeID(), is(second.getNodeID()));
        assertThat(third.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis | Galeichthys felis"));
    }

    @Test
    public void noDuplicatesOnChoppingNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Ariopsis felis", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Ariopsis felis something", null, null);
        assertThat(first.getExternalId(), is(second.getExternalId()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
    }

    @Test
    public void noDuplicatesOnAlternateNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Cliona caribbaea", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Cliona langae", null, null);
        assertThat(first.getExternalId(), is(second.getExternalId()));
        assertThat(first.getPath(), is(second.getPath()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
    }

    @Test
    public void createHomoSapiens() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon("Homo sapiens", null, null);
        assertThat(taxon.getName(), is("Homo sapiens"));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), is(notNullValue()));
        assertThat(taxon.getCommonNames(), is(notNullValue()));

        assertZeroHits(taxonService, "no:match");
    }

    @Test(expected = NodeFactoryException.class)
    public void nameTooShort() throws NodeFactoryException {
        taxonService.getOrCreateTaxon("", null, null);
    }

    private void assertZeroHits(TaxonServiceImpl taxonService, String taxonName) {
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonName(taxonName);
        assertThat(hits.size(), is(0));
    }
}
