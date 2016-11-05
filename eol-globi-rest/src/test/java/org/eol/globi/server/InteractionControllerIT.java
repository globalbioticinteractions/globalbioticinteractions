package org.eol.globi.server;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class InteractionControllerIT extends ITBase {

    @Test
    public void listPreyForPredator() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void distinctTaxa() throws IOException {
        String uri = getURLPrefix() + "taxon";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listSymbiontOf() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/symbiontOf";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listEndoParasiteOf() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/PARASITE_OF";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredatorLocation() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn?lat=12.4&lng=54.4";
        HttpGet httpGet = new HttpGet(uri);
        HttpUtil.addJsonHeaders(httpGet);
        HttpResponse execute = HttpUtil.getHttpClient().execute(httpGet);
        assertThat(execute.getHeaders("Content-Type")[0].getValue(), is("application/json;charset=UTF-8"));
        String response = IOUtils.toString(execute.getEntity().getContent());
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredatorDOT() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn?type=dot";
        HttpGet httpGet = new HttpGet(uri);
        HttpUtil.addJsonHeaders(httpGet);
        HttpResponse execute = HttpUtil.getHttpClient().execute(httpGet);
        assertThat(execute.getHeaders("Content-Type")[0].getValue(), is("text/vnd.graphviz;charset=UTF-8"));
        String response = IOUtils.toString(execute.getEntity().getContent());
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredatorLocationCSV() throws IOException {
        assertCSV(getURLPrefix() + "taxon/Homo%20sapiens/preysOn?type=csv&lat=12.4&lng=54.4");
    }

    @Test
    public void listPreyForPredatorCSV() throws IOException {
        assertCSV(getURLPrefix() + "taxon/Homo%20sapiens/preysOn?type=csv");
    }

    @Test
    public void listPreyForPredatorCSVExtension() throws IOException {
        assertCSV(getURLPrefix() + "taxon/Homo%20sapiens/preysOn.csv");
    }

    private void assertCSV(String uri) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        HttpUtil.addJsonHeaders(httpGet);
        HttpResponse execute = HttpUtil.getHttpClient().execute(httpGet);
        assertThat(execute.getHeaders("Content-Type")[0].getValue(), is("text/csv;charset=UTF-8"));
        String response = IOUtils.toString(execute.getEntity().getContent());
        assertThat(response, not(containsString("columns")));
        assertThat(response, anyOf(containsString("\"" + ResultField.SOURCE_TAXON_NAME + "\""),
                containsString("\"" + ResultField.TARGET_TAXON_NAME + "\"")));
    }

    @Test
    public void listPreyForPredatorObservations() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn?includeObservations=true";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredatorObservationsExternalIdOTT() throws IOException {
        String uri = getURLPrefix() + "taxon/OTT%3A770315/preysOn?includeObservations=true";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, containsString("Homo sapiens"));
    }

    @Test
    public void listPreyForPredatorObservations2() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn/Rattus%20rattus?includeObservations=true";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPreyForPredatorObservationsLocation() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn?includeObservations=true&lat=12.4&lng=34.2";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPredatorForPrey() throws IOException {
        String uri = getURLPrefix() + "taxon/Foraminifera/preyedUponBy";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, containsString("preyedUponBy"));
    }

    @Test
    public void listPredatorForPreyLocation() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preyedUponBy?lat=12.3&lng=23.2";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPredatorForPreyObservations() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preyedUponBy?includeObservations=true";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void listPredatorForPreyObservationsCSV() throws IOException {
        String uri = getURLPrefix() + "taxon/Rattus%20rattus/preyedUponBy?includeObservations=true&type=csv";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, not(containsString("columns")));
        assertThat(response, anyOf(containsString(ResultField.SOURCE_TAXON_NAME.getLabel()),
                containsString(ResultField.TARGET_TAXON_NAME.getLabel()),
                containsString(ResultField.INTERACTION_TYPE.getLabel()),
                containsString(ResultField.LATITUDE.getLabel())));
    }

    @Test
    public void interactionDOT() throws IOException {
        String uri = getURLPrefix() + "interaction?type=dot";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void listPreyObservationsLocation() throws IOException {
        String uri = getURLPrefix() + "taxon/Homo%20sapiens/preysOn?includeObservations=true&lat=12.3&lng=12.5";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

}
