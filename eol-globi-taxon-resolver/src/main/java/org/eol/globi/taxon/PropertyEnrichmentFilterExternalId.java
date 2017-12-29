package org.eol.globi.taxon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnrichmentFilter;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class PropertyEnrichmentFilterExternalId implements PropertyEnrichmentFilter {
    private static final Log LOG = LogFactory.getLog(PropertyEnrichmentFilter.class);
    public static final String EOL_NON_TAXON_PAGES = "http://eol.org/api/collections/1.0/6991.json?per_page=500";

    private final PropertyEnrichmentFilterWithPathOnly propertyEnrichmentFilterWithPathOnly = new PropertyEnrichmentFilterWithPathOnly();

    private Collection<String> exludedEOLIds;

    private Collection<String> getExcludedIds() {
        if (exludedEOLIds == null) {
            exludedEOLIds = new HashSet<>();
            try {
                String response = HttpUtil.executeWithTimer(new HttpGet(EOL_NON_TAXON_PAGES), new BasicResponseHandler());
                JsonNode jsonNode = new ObjectMapper().readTree(response);
                JsonNode collectionItems = jsonNode.get("collection_items");
                for (JsonNode item : collectionItems) {
                    JsonNode objectId = item.get("object_id");
                    if (objectId != null) {
                        exludedEOLIds.add(TaxonomyProvider.ID_PREFIX_EOL + objectId.asText());
                    }
                }

            } catch (IOException e) {
                LOG.error("failed to retrieve excluded taxon list from eol collection [" + EOL_NON_TAXON_PAGES + "]");
            }
        }
        return exludedEOLIds;
    }

    @Override
    public boolean shouldReject(Map<String, String> properties) {
        return propertyEnrichmentFilterWithPathOnly.shouldReject(properties)
                || shouldExcludeExternalId(properties);
    }

    protected boolean shouldExcludeExternalId(Map<String, String> props) {
        String externalId = props.get(PropertyAndValueDictionary.EXTERNAL_ID);
        return getExcludedIds().contains(externalId);
    }

    public void shutdown() {

    }
}
