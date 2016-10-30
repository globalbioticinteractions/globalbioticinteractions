package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PassThroughCorrectionService;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.junit.Ignore;
import org.junit.Test;
import scala.util.Random;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class LinkerGlobalNamesTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Homo sapiens");
        taxonIndex.getOrCreateTaxon("Ariopsis felis");
        taxonIndex.getOrCreateTaxon("Canis lupus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Homo sapiens", 5, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Homo sapiens", 0, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Canis lupus", 5, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Canis lupus", 0, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 7, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 0, taxonIndex, RelTypes.SIMILAR_TO);
    }

    @Test
    public void oneSimilarTaxon() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Homo sapienz");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Homo sapienz", 5, taxonIndex, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Homo sapienz", 0, taxonIndex, RelTypes.SAME_AS);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Gilippus hostilis");
        taxonIndex.getOrCreateTaxon("Euander lacertosus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Gilippus hostilis", 2, taxonIndex, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Euander lacertosus", 2, taxonIndex, RelTypes.SAME_AS);

    }

    @Test
    public void anura() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Anura", 14, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItems("ITIS:173423"
                , "NCBI:8342", "IRMNG:10211", "GBIF:952"
                , "IRMNG:1284513", "GBIF:3242458", "GBIF:3089470"));

    }

    @Test
    @Ignore
    // see https://github.com/GlobalNamesArchitecture/gnparser/issues/291
    public void exactMatchExcludeStrains() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Phytophthora infestans");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Phytophthora infestans", 6, taxonIndex, RelTypes.SAME_AS);

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

        final TaxonNode taxonCreated = taxonIndex.getOrCreateTaxon("Medicago sativa L.");
        assertThat(taxonCreated.getName(), is("Medicago sativa"));
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther(taxonCreated.getName(), 8, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("ITIS:183623"));
        assertThat(ids, hasItem("NCBI:3879"));

    }

    @Test
    public void exactMatchMonodelphisAmericana() throws NodeFactoryException, PropertyEnricherException {
        final TaxonNode taxonCreated = taxonIndex.getOrCreateTaxon("Monodelphis americana");
        assertThat(taxonCreated.getName(), is("Monodelphis americana"));
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther(taxonCreated.getName(), 4, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItems("ITIS:552569", "NCBI:694061", "IRMNG:11060619", "GBIF:2439970"));
    }

    @Test
    public void hasFishBaseLinks() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Ariopsis felis");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Ariopsis felis", 7, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("FBC:FB:SpecCode:947"));
        assertThat(ids, hasItem("INAT_TAXON:94635"));

    }

    @Test
    public void hasSeaLifeBaseLinks() throws NodeFactoryException, PropertyEnricherException {
        taxonIndex.getOrCreateTaxon("Enhydra lutris");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Enhydra lutris", 7, taxonIndex, RelTypes.SAME_AS);

        assertThat(ids, hasItem("FBC:SLB:SpecCode:69195"));

    }

    @Test
    public void lestesExcludeSuspectedHomonyms() throws NodeFactoryException, PropertyEnricherException {
        final PropertyEnricher genus = new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                return new HashMap<String, String>(properties) {
                    {
                        put(PropertyAndValueDictionary.PATH, "Animalia | Insecta | Lestes");
                        put(PropertyAndValueDictionary.PATH_NAMES, "kingdom | class | genus");
                        put(PropertyAndValueDictionary.RANK, "genus");
                        put(PropertyAndValueDictionary.EXTERNAL_ID, "test:123");

                    }
                };
            }

            @Override
            public void shutdown() {

            }
        };
        taxonIndex = new TaxonIndexImpl(genus,
                new PassThroughCorrectionService(), getGraphDb());
        TaxonNode lestes = taxonIndex.getOrCreateTaxon("Lestes");
        assertThat(lestes.getPath(), is("Animalia | Insecta | Lestes"));
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Lestes", 6, taxonIndex, RelTypes.SAME_AS);
        assertThat(ids, hasItems("NCBI:181491", "ITIS:102061", "IRMNG:1320006", "GBIF:7235838", "GBIF:1423980"));
        assertThat(ids, hasItems("INAT_TAXON:89475"));
    }

}
