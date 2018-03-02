package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public class ORCIDResolverImpl implements AuthorIdResolver {
    private static final Log LOG = LogFactory.getLog(ORCIDResolverImpl.class);

    private String baseUrl = "https://pub.orcid.org/v2.0/";

    @Override
    public String findFullName(final String authorURI) throws IOException {
        String fullName = null;
        ObjectMapper mapper = new ObjectMapper();
        String orcId = authorURI.replaceAll("http[s]*://orcid.org/", "");
        HttpGet get = new HttpGet(baseUrl + orcId);
        get.setHeader("Accept", "application/orcid+json");

        BasicResponseHandler handler = new BasicResponseHandler();
        String response = HttpUtil.getHttpClient().execute(get, handler);
        JsonNode jsonNode = mapper.readTree(response);
        JsonNode person = jsonNode.get("person");
        if (person != null) {
            JsonNode name = person.get("name");
            if (name != null) {
                fullName = getValue(name, "given-names") + " " + getValue(name, "family-name");
            }
        }
        return fullName;
    }

    protected String getValue(JsonNode details, String fieldName) {
        JsonNode givenNames = details.get(fieldName);
        String givenNamesValue = "";
        if (givenNames != null) {
            givenNamesValue = givenNames.get("value").getTextValue();
        }
        return givenNamesValue;
    }


    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
