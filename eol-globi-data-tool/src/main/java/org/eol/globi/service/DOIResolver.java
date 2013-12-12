package org.eol.globi.service;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

public class DOIResolver {
    public String findDOIForReference(final String reference) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

                HttpPost post = new HttpPost("http://search.crossref.org/links");
                post.setHeader("Content-Type", "application/json");
                StringEntity entity = new StringEntity(mapper.writeValueAsString(new ArrayList<String>() {{
                    add(reference);
                }}));
                post.setEntity(entity);

                BasicResponseHandler handler = new BasicResponseHandler();
                String response = new DefaultHttpClient().execute(post, handler);
                JsonNode jsonNode = mapper.readTree(response);
                JsonNode results = jsonNode.get("results");
                String doi = null;
                if (jsonNode.get("query_ok").getValueAsBoolean()) {
                    for (JsonNode result : results) {
                        if (result.get("match").getValueAsBoolean()) {
                            doi = result.get("doi").getTextValue();
                        }
                    }
                }
                return doi;
    }
}
