package org.trophic.graph.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class CypherQueryIT {


    @Ignore
    @Test
    public void queryPredatorPreyRelationship() throws IOException {
        // queries live database
        HttpPost post = new HttpPost("http://ec2-50-112-48-206.us-west-2.compute.amazonaws.com:7474/db/data/ext/CypherPlugin/graphdb/execute_query");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        String queryString = "{ \"query\":\"" +
                "START taxon = node:taxons(name=\\\"Syacium papillosum\\\") " +
                "MATCH taxon<-[x:CLASSIFIED_AS]-predatorSpecimen-[:ATE]->preySpecimen-[:CLASSIFIED_AS]->preyTaxon " +
                "WHERE has(preyTaxon.externalId) AND has(taxon.externalId) " +
                "RETURN distinct preyTaxon.externalId, taxon.externalId LIMIT 100\"}";
        post.setEntity(new StringEntity(queryString));

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        HttpClient httpClient = new DefaultHttpClient();
        String response = null;
        response = httpClient.execute(post, responseHandler);
        assertThat(response, containsString("NCBITaxon:195649"));
        assertThat(response, containsString("data"));
    }
}
