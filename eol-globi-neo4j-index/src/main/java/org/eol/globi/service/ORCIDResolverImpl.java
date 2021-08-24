package org.eol.globi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;

public class ORCIDResolverImpl implements AuthorIdResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ORCIDResolverImpl.class);

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
            givenNamesValue = givenNames.get("value").asText();
        }
        return givenNamesValue;
    }


    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

}
