package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExporterTaxaRollupTest extends GraphDBTestCase {

    @Test
    public void export() throws NodeFactoryException, ParseException, IOException {
        NodeFactory nodeFactory1 = createNodeFactory(new PropertyEnricher() {
            @Override
            public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
                HashMap<String, String> enriched = new HashMap<String, String>(properties);
                if ("Homo sapiens".equals(properties.get("name"))) {
                    enriched.put(PropertyAndValueDictionary.PATH, "phylum 1 | genus 1 | species 1");
                    enriched.put(PropertyAndValueDictionary.PATH_IDS, "phylum id 1 | genus id 1 | species id 1");
                    enriched.put(PropertyAndValueDictionary.PATH_NAMES, "phylum | genus | species");
                } else {
                    enriched.put(PropertyAndValueDictionary.PATH, "phylum 2 | family 2 | genus 2 | species 2");
                    enriched.put(PropertyAndValueDictionary.PATH_IDS, "phylum id 2 | family id 2 | genus id 2 | species id 2");
                    enriched.put(PropertyAndValueDictionary.PATH_NAMES, "phylum | family | genus | species");
                }
                return enriched;
            }

            @Override
            public void shutdown() {

            }
        });
        ExportTestUtil.createTestData(null, nodeFactory1);

        StringWriter writer = new StringWriter();
        Study myStudy = nodeFactory1.findStudy("myStudy");
        assertThat(myStudy, is(notNullValue()));
        new ExporterTaxaRollup().doExportStudy(myStudy, writer, true);
        String exported = writer.toString();
        assertThat(exported, containsString("genus id 2,genus 2,,,phylum 2,,,family 2,genus 2,genus,,,,,"));
        assertThat(exported, containsString("genus id 1,genus 1,,,phylum 1,,,,genus 1,genus,,,,,"));
        assertThat(exported, containsString("species id 2,species 2,,,phylum 2,,,family 2,genus 2,species,,,,,"));
        assertThat(exported, containsString("species id 1,species 1,,,phylum 1,,,,genus 1,species,,,,,"));
        assertThat(exported, containsString("family id 2,family 2,,,phylum 2,,,family 2,,family,,,,,"));
        assertThat(exported, not(containsString("phylum id 1,phylum 1,")));
    }

    @Test
    public void collectUniqueExpandTaxa() {
        Map<String, Object> result = new HashMap<String, Object>() {
            {
                put("rank", "the taxon rank");
                put("pathNames", "kingdom | phylum | class | order | family | genus");
                put("path", "the kingdom | the phylum | the class | the order | the family | the genus");
                put("pathIds", "EOL:1 | EOL:2 | EOL:3 | EOL:4 | EOL:5 | EOL:1234");
                put("scientificName", "Some namus");
                put("taxonId", "EOL:1234");
            }
        };

        List<Map<String, Object>> expandedResults = ExporterTaxaRollup.expandTaxonResult(result);

        assertThat(expandedResults.size(), is(3));
        Map<String, Object> genus = expandedResults.get(2);
        assertThat((String) genus.get("rank"), is("genus"));
        assertThat((String) genus.get("scientificName"), is("the genus"));
        assertThat((String) genus.get("path"), is("the kingdom | the phylum | the class | the order | the family | the genus"));
        assertThat((String) genus.get("pathNames"), is("kingdom | phylum | class | order | family | genus"));
        assertThat((String) genus.get("taxonId"), is("EOL:1234"));

        Map<String, Object> family = expandedResults.get(1);
        assertThat((String) family.get("rank"), is("family"));
        assertThat((String) family.get("scientificName"), is("the family"));
        assertThat((String) family.get("path"), is("the kingdom | the phylum | the class | the order | the family"));
        assertThat((String) family.get("taxonId"), is("EOL:5"));

        Map<String, Object> order = expandedResults.get(0);
        assertThat((String) order.get("rank"), is("order"));
        assertThat((String) order.get("scientificName"), is("the order"));
        assertThat((String) order.get("path"), is("the kingdom | the phylum | the class | the order"));
        assertThat((String) order.get("pathNames"), is("kingdom | phylum | class | order"));
        assertThat((String) order.get("taxonId"), is("EOL:4"));

    }


}