package org.trophic.graph.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.trophic.graph.domain.TaxonImage;
import org.trophic.graph.domain.TaxonomyProvider;

import java.io.IOException;

public class EOLTaxonImageService extends BaseService {
    private static final Log LOG = LogFactory.getLog(EOLTaxonImageService.class);

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
        } else {
            throw new UnsupportedOperationException("unsupported taxonomy provider [" + provider + "]");
        }

        String urlString = "http://eol.org/api/search_by_provider/1.0/" + taxonId + ".json?hierarchy_id=" + eolProviderId;
        HttpGet get = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(get);

        String responseString = EntityUtils.toString(response.getEntity());

        LOG.info(responseString);
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

        if (null != eolPageId) {
            String pageUrlString = "http://eol.org/api/pages/1.0/" + eolPageId + ".json?common_names=0&details=0&images=1&videos=0&text=0";
            response = httpClient.execute(new HttpGet(pageUrlString));
            responseString = EntityUtils.toString(response.getEntity());
            LOG.info(responseString);
            if (200 == response.getStatusLine().getStatusCode()) {
                taxonImage = new TaxonImage();
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
        }
        return taxonImage;
    }
}
