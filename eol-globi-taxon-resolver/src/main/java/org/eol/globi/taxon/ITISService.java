package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ITISService implements PropertyEnricher {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see https://data.nbn.org.uk/Documentation/Web_Services/Web_Services-REST/Getting_Records/
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.ITIS.getIdPrefix())) {
            String tsn = externalId.replace(TaxonomyProvider.ID_PREFIX_ITIS, "");
            String acceptedResponse = getResponse("getAcceptedNamesFromTSN", "tsn=" + tsn);
            String[] split = StringUtils.splitByWholeSeparator(acceptedResponse, "acceptedTsn>");
            if (split != null && split.length > 1) {
                tsn = split[1].split("<")[0];
            }
            String fullHierarchy = getResponse("getFullHierarchyFromTSN", "tsn=" + tsn);
            enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_ITIS + tsn);
            String taxonNames = ServiceUtil.extractPath(fullHierarchy, "taxonName", "");
            enriched.put(PropertyAndValueDictionary.PATH, taxonNames);
            String rankNames = ServiceUtil.extractPath(fullHierarchy, "rankName", "");
            enriched.put(PropertyAndValueDictionary.PATH_NAMES, rankNames);
            enriched.put(PropertyAndValueDictionary.PATH_IDS, ServiceUtil.extractPath(fullHierarchy, "tsn", "ITIS:"));

            setPropertyToLastValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
            setPropertyToLastValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
        }
        return enriched;
    }

    protected void setPropertyToLastValue(String propertyName, String taxonNames, Map<String, String> enriched) {
        if (taxonNames != null) {
            String[] split1 = taxonNames.split("\\" + CharsetConstant.SEPARATOR_CHAR);
            enriched.put(propertyName, split1[split1.length - 1].trim());
        }
    }

    private String getResponse(String methodName, String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("http", null, "www.itis.gov", 80, "/ITISWebService/services/ITISService/" + methodName, queryString, null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpUtil.executeWithTimer(get, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to execute query to [" + uri.toString() + "]", e);
        }
        return response;
    }

    public void shutdown() {

    }
}
