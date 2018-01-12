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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NCBIService implements PropertyEnricher {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.ncbi.nlm.nih.gov/books/NBK25500/
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.NCBI.getIdPrefix())) {
            String tsn = externalId.replace(TaxonomyProvider.ID_PREFIX_NCBI, "");
            if (tsn.matches("\\d+")) {
                String fullHierarchy = getResponse("db=taxonomy&id=" + tsn);
                if (fullHierarchy.contains("<Taxon>")) {
                    parseAndPopulate(enriched, tsn, fullHierarchy);
                }
            }
        }
        return enriched;
    }

    protected void parseAndPopulate(Map<String, String> enriched, String tsn, String fullHierarchy) throws PropertyEnricherException {
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_NCBI + tsn);
        String taxonNames = XmlUtil.extractPath(fullHierarchy, "ScientificName", "");

        enriched.put(PropertyAndValueDictionary.PATH, firstWillBeLast(taxonNames));
        String rankNames = XmlUtil.extractPath(fullHierarchy, "Rank", "");
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, firstWillBeLast(rankNames).replaceAll("no rank", ""));
        enriched.put(PropertyAndValueDictionary.PATH_IDS, firstWillBeLast(XmlUtil.extractPath(fullHierarchy, "TaxId", TaxonomyProvider.ID_PREFIX_NCBI)));
        String genBankCommonName = XmlUtil.extractPath(fullHierarchy, "GenbankCommonName", "", " @en");
        String commonName = XmlUtil.extractPath(fullHierarchy, "CommonName", "", " @en");
        enriched.put(PropertyAndValueDictionary.COMMON_NAMES, commonName + CharsetConstant.SEPARATOR + genBankCommonName);

        setPropertyToFirstValue(PropertyAndValueDictionary.NAME, taxonNames, enriched);
        setPropertyToFirstValue(PropertyAndValueDictionary.RANK, rankNames, enriched);
    }

    protected void setPropertyToFirstValue(String propertyName, String taxonNames, Map<String, String> enriched) {
        if (taxonNames != null) {
            String[] split1 = taxonNames.split("\\" + CharsetConstant.SEPARATOR_CHAR);
            enriched.put(propertyName, split1[0].trim());
        }
    }

    protected String firstWillBeLast(String taxonNames) {
        String transformedNames = taxonNames;
        if (taxonNames != null) {
            String[] split1 = taxonNames.split("\\" + CharsetConstant.SEPARATOR_CHAR);
            List<String> list1 = Arrays.asList(split1);
            Collections.rotate(list1, -1);
            transformedNames = StringUtils.join(list1, CharsetConstant.SEPARATOR);
            transformedNames = transformedNames.replaceAll("\\s+", " ").trim();
        }
        return transformedNames;
    }

    private String getResponse(String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("https", null, "eutils.ncbi.nlm.nih.gov", 443, "/entrez/eutils/efetch.fcgi", queryString, null);
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
