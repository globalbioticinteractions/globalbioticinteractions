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
import java.util.List;
import java.util.Map;

public class ITISService implements PropertyEnricher {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (isNumericITISTsn(externalId)) {
            String tsn = externalId.replace(TaxonomyProvider.ID_PREFIX_ITIS, "");
            String acceptedResponse = getResponse("getAcceptedNamesFromTSN", "tsn=" + tsn);
            String[] split = StringUtils.splitByWholeSeparator(acceptedResponse, "acceptedTsn>");
            if (split != null && split.length > 1) {
                tsn = split[1].split("<")[0];
            }
            String fullHierarchy = getResponse("getFullHierarchyFromTSN", "tsn=" + tsn);
            final String taxonId = TaxonomyProvider.ID_PREFIX_ITIS + tsn;
            enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, taxonId);
            final List<String> pathIds = XmlUtil.extractPathNoJoin(fullHierarchy, "tsn", "ITIS:");
            List<String> pathIdsTail = pathIds;
            if (pathIdsTail.size() > 1) {
                pathIdsTail = pathIds.subList(1, pathIds.size());
            }
            final int taxonIdIndex = pathIdsTail.lastIndexOf(taxonId);
            enriched.put(PropertyAndValueDictionary.PATH_IDS, subJoin(taxonIdIndex, pathIdsTail));

            String taxonNames = subJoin(taxonIdIndex, XmlUtil.extractPathNoJoin(fullHierarchy, "taxonName", ""));
            enriched.put(PropertyAndValueDictionary.PATH, taxonNames);

            String rankNames = subJoin(taxonIdIndex, XmlUtil.extractPathNoJoin(fullHierarchy, "rankName", ""));
            enriched.put(PropertyAndValueDictionary.PATH_NAMES, rankNames);

            setPropertyToLastValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
            setPropertyToLastValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
        }
        return enriched;
    }

    public boolean isNumericITISTsn(String externalId) {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ITIS.getIdPrefix())
                && StringUtils.isNumeric(externalId.replace(TaxonomyProvider.ID_PREFIX_ITIS, ""));
    }

    public String subJoin(int taxonIdIndex, List<String> taxonNames) {
        List<String> subList = taxonNames;
        if (taxonIdIndex != -1 && taxonNames.size() > taxonIdIndex) {
            subList = taxonNames.subList(0, taxonIdIndex + 1);
        }
        return StringUtils.join(subList, CharsetConstant.SEPARATOR);
    }

    protected static void setPropertyToLastValue(String propertyName, String taxonNames, Map<String, String> enriched) {
        if (taxonNames != null) {
            String[] split1 = taxonNames.split("\\" + CharsetConstant.SEPARATOR_CHAR);
            enriched.put(propertyName, split1[split1.length - 1].trim());
        }
    }

    private String getResponse(String methodName, String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("https", null, "www.itis.gov", 443, "/ITISWebService/services/ITISService/" + methodName, queryString, null);
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
