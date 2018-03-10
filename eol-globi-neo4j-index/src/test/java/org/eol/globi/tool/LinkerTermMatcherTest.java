package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.taxon.TaxonCacheService;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class LinkerTermMatcherTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Ariopsis felis", null));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Canis lupus", null));

        new LinkerTermMatcher(getGraphDb()).link();

        LinkerTestUtil.assertHasOther("Homo sapiens", 5, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Homo sapiens", 0, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Canis lupus", 6, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Canis lupus", 0, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 8, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 0, taxonIndex, RelTypes.SIMILAR_TO);
    }

    @Test
    public void oneSimilarTaxon() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapienz", null));

        new LinkerTermMatcher(getGraphDb()).link();

        LinkerTestUtil.assertHasOther("Homo sapienz", 5, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Homo sapienz", 0, taxonIndex, RelTypes.SAME_AS);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Gilippus hostilis", null));
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Euander lacertosus", null));

        new LinkerTermMatcher(getGraphDb()).link();

        LinkerTestUtil.assertHasOther("Gilippus hostilis", 3, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Euander lacertosus", 3, taxonIndex, RelTypes.SAME_AS);

    }

    @Test
    public void homoSapiensCachedTaxa() throws NodeFactoryException, PropertyEnricherException {

        taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null));

        TaxonCacheService taxonCacheService = new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv", "classpath:/org/eol/globi/taxon/taxonMap.tsv");
        new LinkerTermMatcher(getGraphDb(), taxonCacheService).link();

        LinkerTestUtil.assertHasOther("Homo sapiens", 2, taxonIndex, RelTypes.SAME_AS);

    }

    @Test
    public void anura() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Anura", null));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther("Anura", 8, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItems("ITIS:173423"
                , "NCBI:8342", "IRMNG:10211", "GBIF:952"
                , "IRMNG:1284513", "GBIF:3242458", "OTT:991547"));

        Collection<String> synonymIds = LinkerTestUtil.assertHasOther("Anura", 9, taxonIndex, RelTypes.SYNONYM_OF);

        assertThat(synonymIds, not(hasItems("ITIS:173423"
                , "NCBI:8342", "IRMNG:10211", "GBIF:952"
                , "IRMNG:1284513", "GBIF:3242458")));

    }

    @Test
    @Ignore
    // see https://github.com/GlobalNamesArchitecture/gnparser/issues/291
    public void exactMatchExcludeStrains() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Phytophthora infestans", null));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther("Phytophthora infestans", 6, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("NCBI:4787"));
        assertThat(ids, not(hasItem("NCBI:403677")));

    }

    @Test
    // see //see https://github.com/jhpoelen/eol-globi-data/issues/249
    public void exactMatchMedicagoSativa() throws NodeFactoryException, PropertyEnricherException {
        Taxon nbnTaxon = new TaxonImpl("Medicago sativa", "NBN:NBNSYS0000013971");
        nbnTaxon.setPath("Biota | Plantae | Tracheophyta | Magnoliopsida | Fabales | Fabaceae | Medicago | Medicago sativa");
        nbnTaxon.setPathNames("Unranked | Kingdom | Phylum | Class | Order | Family | Genus | Species");
        taxonIndex.getOrCreateTaxon(nbnTaxon);

        final Taxon taxonCreated = taxonIndex.getOrCreateTaxon(new TaxonImpl("Medicago sativa L.", null));
        assertThat(taxonCreated.getName(), is("Medicago sativa L."));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther(taxonCreated.getName(), 7, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("ITIS:183623"));
        assertThat(ids, hasItem("NCBI:3879"));
        assertThat(ids, not(hasItem("NCBI:56066")));

    }

    @Test
    public void exactMatchMonodelphisAmericana() throws NodeFactoryException, PropertyEnricherException {
        final Taxon taxonCreated = taxonIndex.getOrCreateTaxon(new TaxonImpl("Monodelphis americana", null));
        assertThat(taxonCreated.getName(), is("Monodelphis americana"));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther(taxonCreated.getName(), 5, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItems("ITIS:552569", "NCBI:694061", "IRMNG:11060619", "GBIF:2439970", "OTT:446727"));
    }

    @Test
    public void hasFishBaseLinks() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Ariopsis felis", null));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther("Ariopsis felis", 8, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("FBC:FB:SpecCode:947"));
        assertThat(ids, hasItem("INAT_TAXON:94635"));

    }

    @Test
    public void hasSeaLifeBaseLinks() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon(new TaxonImpl("Enhydra lutris", null));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther("Enhydra lutris", 8, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("FBC:SLB:SpecCode:69195"));

    }

    private Taxon addProps(Taxon taxon) {
        taxon.setPath("Animalia | Insecta | Lestes");
        taxon.setPathNames("kingdom | class | genus");
        taxon.setRank("genus");
        taxon.setExternalId("test:123");
        return taxon;
    }

    @Test
    public void lestesExcludeSuspectedHomonyms() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex = new NonResolvingTaxonIndex(getGraphDb());
        Taxon lestes = taxonIndex.getOrCreateTaxon(addProps(new TaxonImpl("Lestes", null)));
        assertThat(lestes.getPath(), is("Animalia | Insecta | Lestes"));
        new LinkerTermMatcher(getGraphDb()).link();
        Collection<String> ids = LinkerTestUtil.assertHasOther("Lestes", 7, taxonIndex, RelTypes.SAME_AS);
        assertThat(ids, hasItems("NCBI:181491", "ITIS:102061", "IRMNG:1320006", "GBIF:1423980", "OTT:1090993"));
        assertThat(ids, hasItems("INAT_TAXON:89475"));
    }

}
