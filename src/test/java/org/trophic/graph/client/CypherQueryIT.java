package org.trophic.graph.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

/**
 * Testing ground for cypher queries.
 */
@Ignore
public class CypherQueryIT {


    public static final String REMOTE_HOST = "http://ec2-50-112-48-206.us-west-2.compute.amazonaws.com";
    public static final String LOCAL_HOST = "http://localhost";
    public static final String REMOTE_HOST_HUGE = "http://ec2-50-112-47-249.us-west-2.compute.amazonaws.com";

    @Test
    public void queryPredatorPreyRelationshipRemote() throws IOException {
        // queries live database
        assertPredatorPrey(REMOTE_HOST, "Syacium papillosum");
    }

    @Test
    public void queryPredatorPreyRelationshipLocal() throws IOException {
        // queries live database
        assertPredatorPrey(LOCAL_HOST, "Syacium papillosum");
    }

    @Test
    public void queryWhatDoWeEat() throws IOException {
        assertPredatorPrey(LOCAL_HOST, "Homo sapiens");
    }

    @Test
    public void queryWhatDoWeEatHugeAwsHost() throws IOException {
        assertPredatorPrey(REMOTE_HOST_HUGE, "Homo sapiens");
    }


    private void assertPredatorPrey(String host, String predatorTaxon) throws IOException {
        String queryJson = "{ \"query\":\"" +
                "START taxon = node:taxons(name=\\\"" + predatorTaxon + "\\\") " +
                "MATCH taxon<-[x:CLASSIFIED_AS]-predatorSpecimen-[:ATE]->preySpecimen-[:CLASSIFIED_AS]->preyTaxon " +
                "RETURN distinct(preyTaxon.name)\"}";

        String response = postJson(queryJson, host);
        assertThat(response, containsString("NCBITaxon:195649"));
        assertThat(response, containsString("data"));
    }

    @Test
    public void studyStatsRemoteHuge() throws IOException {
        assertPredatorQuery(REMOTE_HOST_HUGE);
    }

    @Test
    public void studyStatsRemote() throws IOException {
        assertPredatorQuery(REMOTE_HOST);
    }

    @Test
    public void studyStatLocal() throws IOException {
        assertPredatorQuery(LOCAL_HOST);
    }

    private void assertPredatorQuery(String host) throws IOException {
        String queryJson = "{ \"query\":\"" +
                "START study=node:studies('*:*') " +
                "MATCH study-[:COLLECTED]->predator-[:CLASSIFIED_AS]->taxon " +
                "RETURN study.title, count(distinct(taxon.name))\"}";
        String response = postJson(queryJson, host);
        assertThat(response, is("bla"));
        assertThat(response, containsString("wrast"));
        assertThat(response, containsString("data"));
    }

    @Test
    public void studyPreyStatsRemote() throws IOException {
        assertPreyQuery(REMOTE_HOST);
    }

    @Test
    public void studyPreyStatsRemoteHuge() throws IOException {
        assertPreyQuery(REMOTE_HOST_HUGE);
    }


    @Test
    public void studyPreyStatsLocal() throws IOException {
        assertPreyQuery(LOCAL_HOST);
    }


    private void assertPreyQuery(String host) throws IOException {
        String queryJson = "{ \"query\":\"" +
                "START study=node:studies('title:*') " +
                "MATCH study-[:COLLECTED]->predator-[ateRel:ATE|PREYS_UPON|PARASITE_OF|HAS_HOST|INTERACTS_WITH]->prey-[:CLASSIFIED_AS]->taxon " +
                "RETURN study.title, count(ateRel), count(distinct(taxon))\"}";
        String response = postJson(queryJson, host);
        assertThat(response, is("bla"));
        assertThat(response, containsString("wrast"));
        assertThat(response, containsString("data"));
    }

    private String postJson(String queryJson, String host) throws IOException {
        HttpPost post = new HttpPost(host + ":7474/db/data/ext/CypherPlugin/graphdb/execute_query");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Accept", "application/json");
        post.setEntity(new StringEntity(queryJson));

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        HttpClient httpClient = new DefaultHttpClient();
        String response = null;
        response = httpClient.execute(post, responseHandler);
        return response;
    }
}
