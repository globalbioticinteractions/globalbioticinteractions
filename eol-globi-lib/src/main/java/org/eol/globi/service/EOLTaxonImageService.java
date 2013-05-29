package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;

public class EOLTaxonImageService extends BaseHttpClientService {
    private static final Log LOG = LogFactory.getLog(EOLTaxonImageService.class);

    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        TaxonImage image = null;
        if (externalId == null) {
            LOG.warn("cannot lookup image for null externalId");
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_EOL)) {
            image = lookupImageURLs(TaxonomyProvider.EOL, externalId.replace(TaxonomyProvider.ID_PREFIX_EOL, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_WORMS)) {
            image = lookupImageURLs(TaxonomyProvider.WORMS, externalId.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_ITIS)) {
            image = lookupImageURLs(TaxonomyProvider.ITIS, externalId.replace(TaxonomyProvider.ID_PREFIX_ITIS, ""));
        }
        return image;
    }

    public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
        TaxonImage taxonImage = null;
        String eolPageId = null;
        String eolProviderId = "627";

        if (TaxonomyProvider.ITIS.equals(provider)) {
            eolProviderId = "903";
        } else if (TaxonomyProvider.NCBI.equals(provider)) {
            eolProviderId = "759";
        } else if (TaxonomyProvider.WORMS.equals(provider)) {
            eolProviderId = "123";
        } else if (TaxonomyProvider.EOL.equals(provider)) {
            // no need to lookup, because the page id is already in the taxon id
            eolPageId = taxonId.replace(TaxonomyProvider.ID_PREFIX_EOL, "");
        } else {
            throw new UnsupportedOperationException("unsupported taxonomy provider [" + provider + "]");
        }

        if (eolPageId == null) {
            eolPageId = lookupEOLPageId(taxonId, eolPageId, eolProviderId);
        }

        HttpResponse response;
        String responseString;

        PageInfo pageInfo;

        if (null != eolPageId) {
            pageInfo = getPageInfo(eolPageId);

            if (null != pageInfo) {
                String pageUrlString = "http://eol.org/api/data_objects/1.0/" + pageInfo.getImageObjectId() + ".json";
                DefaultHttpClient httpClient = new DefaultHttpClient();
                try {
                    response = httpClient.execute(new HttpGet(pageUrlString));
                    responseString = EntityUtils.toString(response.getEntity());
                    if (200 == response.getStatusLine().getStatusCode()) {
                        taxonImage = new TaxonImage();
                        taxonImage.setInfoURL(ExternalIdUtil.infoURLForExternalId(TaxonomyProvider.ID_PREFIX_EOL + eolPageId));
                        taxonImage.setEOLPageId(eolPageId);
                        taxonImage.setCommonName(pageInfo.getCommonName());
                        taxonImage.setScientificName(pageInfo.getScientificName());
                        enrichTaxonWithImageInfo(taxonImage, responseString);
                    }
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }
        return taxonImage;
    }

    private PageInfo getPageInfo(String eolPageId) throws IOException {
        HttpResponse response;
        PageInfo pageInfo = new PageInfo();
        String responseString;
        String pageUrlString = "http://eol.org/api/pages/1.0/" + eolPageId + ".json?images=1&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=false&common_names=true&references=false&vetted=0&cache_ttl=";

        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet(pageUrlString);
            response = httpClient.execute(request);
            responseString = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
            if (200 == response.getStatusLine().getStatusCode()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode array = mapper.readTree(responseString);
                JsonNode dataObjects = array.findValue("dataObjects");
                if (dataObjects != null && dataObjects.size() > 0) {
                    JsonNode dataObject = dataObjects.get(0);
                    if (dataObject.has("identifier")) {
                        pageInfo.setImageObjectId(dataObject.get("identifier").getValueAsText());
                    }
                }
                JsonNode commonNames = array.findValue("vernacularNames");
                if (commonNames != null && commonNames.size() > 0) {
                    for (int i = 0; i < commonNames.size(); i++) {
                        JsonNode commonNameNode = commonNames.get(i);
                        if (commonNameNode.has("eol_preferred") && commonNameNode.has("language")) {
                            String language = commonNameNode.get("language").getTextValue();
                            if ("en".equals(language) && commonNameNode.has("vernacularName")) {
                                String vernacularName = commonNameNode.get("vernacularName").getTextValue();
                                String commonName = vernacularName.replaceAll("\\(.*\\)", "");
                                String capitalize = WordUtils.capitalize(commonName);

                                pageInfo.setCommonName(capitalize.replaceAll("\\sAnd\\s", " and "));
                                break;
                            }
                        }
                    }
                }

                JsonNode taxonConceptsNode = array.findValue("taxonConcepts");
                if (taxonConceptsNode != null && taxonConceptsNode.size() > 0) {
                    for (int i = 0; i < taxonConceptsNode.size(); i++) {
                        JsonNode taxonConcept = taxonConceptsNode.get(i);
                        if (taxonConcept.has("canonicalForm")) {
                            pageInfo.setScientificName(taxonConcept.get("canonicalForm").getTextValue());
                            break;
                        }
                    }
                }

            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return pageInfo;
    }

    protected void enrichTaxonWithImageInfo(TaxonImage taxonImage, String responseString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree(responseString);
        JsonNode eolMediaURL = array.findValue("eolMediaURL");
        if (eolMediaURL != null) {
            taxonImage.setImageURL(eolMediaURL.getValueAsText());
        }

        JsonNode eolThumbnailURL = array.findValue("eolThumbnailURL");
        if (eolThumbnailURL != null) {
            taxonImage.setThumbnailURL(eolThumbnailURL.getValueAsText());
        }
    }

    private String lookupEOLPageId(String taxonId, String eolPageId, String eolProviderId) throws IOException {
        String urlString = "http://eol.org/api/search_by_provider/1.0/" + taxonId + ".json?hierarchy_id=" + eolProviderId + "&cache_ttl=3600";
        HttpGet get = new HttpGet(urlString);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(get);

            String responseString = EntityUtils.toString(response.getEntity());

            if (200 == response.getStatusLine().getStatusCode()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseString);
                if (jsonNode.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) jsonNode;
                    if (arrayNode.size() > 0) {
                        JsonNode firstNode = arrayNode.get(0);
                        JsonNode eol_page_id = firstNode.get("eol_page_id");
                        eolPageId = eol_page_id.getValueAsText();
                    }
                }
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return eolPageId;
    }

    private class PageInfo {
        private String imageObjectId;
        private String commonName;
        private String scientificName;

        public void setImageObjectId(String imageObjectId) {
            this.imageObjectId = imageObjectId;
        }

        public String getImageObjectId() {
            return imageObjectId;
        }

        public void setCommonName(String commonName) {
            this.commonName = commonName;
        }

        public String getCommonName() {
            return commonName;
        }


        public void setScientificName(String scientificName) {
            this.scientificName = scientificName;
        }

        public String getScientificName() {
            return scientificName;
        }
    }
}
