package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EOLTaxonImageService implements ImageSearch {
    private static final Log LOG = LogFactory.getLog(EOLTaxonImageService.class);

    public static final Map<TaxonomyProvider, String> EOL_TAXON_PROVIDER_MAP =  Collections.unmodifiableMap(new HashMap<TaxonomyProvider, String>() {{
        put(TaxonomyProvider.ITIS, "903");
        put(TaxonomyProvider.NCBI, "1172");
        put(TaxonomyProvider.WORMS, "123");
        put(TaxonomyProvider.GBIF, "800");
        put(TaxonomyProvider.INDEX_FUNGORUM, "596");
    }});

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
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_NCBI)) {
            image = lookupImageURLs(TaxonomyProvider.NCBI, externalId.replace(TaxonomyProvider.ID_PREFIX_NCBI, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_GBIF)) {
            image = lookupImageURLs(TaxonomyProvider.GBIF, externalId.replace(TaxonomyProvider.ID_PREFIX_GBIF, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_INDEX_FUNGORUM)) {
            image = lookupImageURLs(TaxonomyProvider.INDEX_FUNGORUM, externalId.replace(TaxonomyProvider.ID_PREFIX_INDEX_FUNGORUM, ""));
        }

        if (image == null) {
            String infoURL = ExternalIdUtil.urlForExternalId(externalId);
            if (StringUtils.isNotBlank(infoURL)) {
                image = new TaxonImage();
                image.setInfoURL(infoURL);
            }
        }
        return image;
    }

    public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
        TaxonImage taxonImage = null;
        String eolPageId = null;

        String eolProviderId = EOL_TAXON_PROVIDER_MAP.get(provider);
        if (StringUtils.isBlank(eolProviderId)) {
            if (TaxonomyProvider.EOL.equals(provider)) {
                // no need to lookup, because the page id is already in the taxon id
                eolPageId = taxonId.replace(TaxonomyProvider.ID_PREFIX_EOL, "");
            } else {
                throw new UnsupportedOperationException("unsupported taxonomy provider [" + provider + "]");
            }
        }

        if (eolPageId == null) {
            eolPageId = lookupEOLPageId(taxonId, eolProviderId);
        }

        if (null != eolPageId) {
            PageInfo pageInfo = getPageInfo(eolPageId);
            if (null != pageInfo) {
                taxonImage = new TaxonImage();
                String infoURL = ExternalIdUtil.urlForExternalId(TaxonomyProvider.EOL.getIdPrefix() + eolPageId);
                taxonImage.setInfoURL(infoURL);
                taxonImage.setPageId(eolPageId);
                taxonImage.setCommonName(pageInfo.getCommonName());
                taxonImage.setScientificName(pageInfo.getScientificName());
                taxonImage.setThumbnailURL(pageInfo.getThumbnailURL());
                taxonImage.setThumbnailURL(pageInfo.getThumbnailURL());
                taxonImage.setImageURL(pageInfo.getImageURL());
            }
        }
        return taxonImage;
    }

    private PageInfo getPageInfo(String eolPageId) throws IOException {
        String pageUrlString = "http://eol.org/api/pages/1.0/" + eolPageId + ".json?images=1&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=true&common_names=true&references=false&vetted=0&cache_ttl=";
        HttpGet request = new HttpGet(pageUrlString);
        try {
            HttpResponse response = HttpUtil.getFailFastHttpClient().execute(request);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            return 200 == response.getStatusLine().getStatusCode() ? parsePageInfo(responseString) : null;
        } finally {
            request.releaseConnection();
        }
    }

    private PageInfo parsePageInfo(String responseString) throws IOException {
        PageInfo pageInfo = new PageInfo();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree(responseString);
        JsonNode dataObjects = array.findValue("dataObjects");
        for (JsonNode dataObject : dataObjects) {
            String dataType = dataObject.has("dataType") ? dataObject.get("dataType").asText() : "";
            if ("http://purl.org/dc/dcmitype/StillImage".equals(dataType)) {
                if (dataObject.has("eolMediaURL")) {
                    pageInfo.setImageURL(dataObject.get("eolMediaURL").asText());
                }
                if (dataObject.has("eolThumbnailURL")) {
                    pageInfo.setThumbnailURL(dataObject.get("eolThumbnailURL").asText());
                }

                break;
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
        return pageInfo;
    }


    private String lookupEOLPageId(String taxonId, String eolProviderId) throws IOException {
        String eolPageId = null;
        String urlString = "http://eol.org/api/search_by_provider/1.0/" + taxonId + ".json?hierarchy_id=" + eolProviderId + "&cache_ttl=3600";
        HttpGet get = new HttpGet(urlString);
        try {
            HttpResponse response = HttpUtil.getHttpClient().execute(get);
            String responseString = EntityUtils.toString(response.getEntity());
            if (200 == response.getStatusLine().getStatusCode()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseString);
                if (jsonNode.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) jsonNode;
                    if (arrayNode.size() > 0) {
                        JsonNode firstNode = arrayNode.get(0);
                        JsonNode eol_page_id = firstNode.get("eol_page_id");
                        eolPageId = eol_page_id.asText();
                    }
                }
            }
            return eolPageId;
        } finally {
            get.releaseConnection();
        }
    }

    public void shutdown() {

    }

    private class PageInfo {
        private String commonName;
        private String scientificName;
        private String imageURL;
        private String thumbnailURL;

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

        public void setImageURL(String imageURL) {
            this.imageURL = imageURL;
        }

        public String getImageURL() {
            return imageURL;
        }

        public void setThumbnailURL(String thumbnailURL) {
            this.thumbnailURL = thumbnailURL;
        }

        public String getThumbnailURL() {
            return thumbnailURL;
        }
    }
}
