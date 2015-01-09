package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class RollUpTest extends GraphDBTestCase {
    protected Study createStudy() throws NodeFactoryException, ParseException {
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

        Study myStudy = nodeFactory1.findStudy("myStudy");
        assertThat(myStudy, is(notNullValue()));
        return myStudy;
    }
}
