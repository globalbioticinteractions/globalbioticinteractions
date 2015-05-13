package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class LinkerGlobalNamesTest extends GraphDBTestCase {

    @Test
    public void threeTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Ariopsis felis");
        nodeFactory.getOrCreateTaxon("Canis lupus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Homo sapiens", 4, nodeFactory, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Homo sapiens", 0, nodeFactory, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Canis lupus", 4, nodeFactory, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Canis lupus", 0, nodeFactory, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 5, nodeFactory, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Ariopsis felis", 0, nodeFactory, RelTypes.SIMILAR_TO);
    }

    @Test
    public void oneSimilarTaxon() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Homo sapienz");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Homo sapienz", 4, nodeFactory, RelTypes.SIMILAR_TO);
        LinkerTestUtil.assertHasOther("Homo sapienz", 0, nodeFactory, RelTypes.SAME_AS);

    }

    @Test
    public void australianTaxa() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Gilippus hostilis");
        nodeFactory.getOrCreateTaxon("Euander lacertosus");

        new LinkerGlobalNames().link(getGraphDb());

        LinkerTestUtil.assertHasOther("Gilippus hostilis", 2, nodeFactory, RelTypes.SAME_AS);
        LinkerTestUtil.assertHasOther("Euander lacertosus", 2, nodeFactory, RelTypes.SAME_AS);

    }

    @Test
    public void anura() throws NodeFactoryException, PropertyEnricherException {
        nodeFactory.getOrCreateTaxon("Anura");
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Anura", 14, nodeFactory, RelTypes.SAME_AS);

        assertThat(ids, hasItems("ITIS:173423"
                , "NCBI:8342", "IRMNG:10211", "GBIF:952"
                , "IRMNG:1284513", "GBIF:3242458", "GBIF:3089470"));

    }

    @Test
    public void lestesExcludeSuspectedHomonyms() throws NodeFactoryException, PropertyEnricherException {
        NodeFactory nodeFactory = createNodeFactory(new PropertyEnricher() {
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
        });
        TaxonNode lestes = nodeFactory.getOrCreateTaxon("Lestes");
        assertThat(lestes.getPath(), is("Animalia | Insecta | Lestes"));
        new LinkerGlobalNames().link(getGraphDb());
        List<String> ids = LinkerTestUtil.assertHasOther("Lestes", 5, nodeFactory, RelTypes.SAME_AS);
        assertThat(ids, hasItems("NCBI:181491", "ITIS:102061", "IRMNG:1320006", "GBIF:7235838", "GBIF:1423980"));
    }

}
