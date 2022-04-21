package org.eol.globi.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.util.HttpUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class ImageServiceIT extends ITBase {

    @Test
    public void findImagesForExternalId() throws IOException {
        String uri = getURLPrefix() + "images/EOL:276287";
        assertThat(HttpUtil.getRemoteJson(uri), CoreMatchers.containsString("Oospila albicoma"));
    }

    @Test
    public void imagesForName() throws IOException {
        String uri = getURLPrefix() + "imagesForName/Homo%20sapiens";
        assertThat(HttpUtil.getRemoteJson(uri), is(notNullValue()));
    }

    @Test
    public void imagesForNameNCBI() throws IOException {
        String uri = getURLPrefix() + "imagesForName/NCBI:1000587";
        String remoteJson = HttpUtil.getRemoteJson(uri);
        JsonNode jsonNode = new ObjectMapper().readTree(remoteJson);
        assertThat(jsonNode.get("scientificName").asText(), is("Huitzilac virus"));
    }

    @Test
    public void imagesForPlaziConceptName() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=http%3A%2F%2Ftaxon-concept.plazi.org%2Fid%2FAnimalia%2FCaridae_Dana_1852";
        assertThat(HttpUtil.getRemoteJson(uri), is(notNullValue()));
    }

    @Test
    public void imagesForPlaziConceptName2() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=http%3A%2F%2Ftaxon-concept.plazi.org%2Fid%2FAnimalia%2FCaridae_Dana_1852";
        assertThat(HttpUtil.getRemoteJson(uri), is(notNullValue()));
    }

    @Test
    public void imagesForPlaziTreatmentName() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=http%3A%2F%2Ftreatment.plazi.org%2Fid%2FEA4F8781FFD5FFB3FF0C7606E774FCB3";
        assertThat(HttpUtil.getRemoteJson(uri), is(notNullValue()));
    }

    @Test(expected = HttpResponseException.class)
    public void imagesForNonExistentName() throws IOException {
        String uri = getURLPrefix() + "imagesForName/Donald%20duckus";
        try {
            HttpUtil.getRemoteJson(uri);
        } catch (HttpResponseException ex) {
            assertThat(ex.getStatusCode(), is(HttpStatus.NOT_FOUND.value()));
            throw ex;
        }
    }

    @Test
    public void imagesForNameLongName() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=Influenza%20A%20virus%20(A%2FIndia%2F77302%2F2001(H1N2))";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForEOLExternalIdInsecta() throws IOException {
        String uri = getURLPrefix() + "imagesForName?externalId=EOL:344";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForInsecta() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=Insecta";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForPlantae() throws IOException {
        String uri = getURLPrefix() + "imagesForName?name=Plantae";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    protected String getRemoteJson(String uri) throws IOException {
        return HttpUtil.getRemoteJson(uri);
    }

    @Test(expected = HttpResponseException.class)
    public void imagesForNameLongNameThatSpringDoesntLike() throws IOException {
        String uri = getURLPrefix() + "imagesForName/Influenza%20A%20virus%20(A%2FIndia%2F77302%2F2001(H1N2))";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForNameLongNameOther() throws IOException {
        String uri = getURLPrefix() + "imagesForNames?name=Influenza%20A%20virus%20(A%2FIndia%2F77302%2F2001(H1N2))";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForNames() throws IOException {
        String uri = getURLPrefix() + "imagesForNames?name=Homo%20sapiens&name=Ariopsis%20felis";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test
    public void imagesForNCBIBatWithImage() throws IOException {
        String uri = getURLPrefix() + "imagesForNames?name=NCBI:59451";
        String response = getRemoteJson(uri);
        assertThat(response, is(notNullValue()));
    }

    @Test(expected = HttpResponseException.class)
    public void imagesForNamesNoParams() throws IOException {
        String uri = getURLPrefix() + "imagesForNames";
        try {
            getRemoteJson(uri);
        } catch (HttpResponseException ex) {
            assertThat(ex.getStatusCode(), is(HttpStatus.BAD_REQUEST.value()));
            throw ex;
        }
    }

}
