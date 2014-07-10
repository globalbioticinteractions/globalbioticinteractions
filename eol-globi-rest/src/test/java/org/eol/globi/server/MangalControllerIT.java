package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpClient;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class MangalControllerIT extends ITBase {

    @Test
    public void listTaxa() throws IOException {
        String uri = getURLPrefix() + "mangal/taxa/?format=json";

        JsonNode taxonList = new ObjectMapper().readTree(HttpClient.httpGet(uri));

        JsonNode meta = taxonList.get("meta");
        for (String field : new String[]{"offset", "limit", "next", "previous", "total_count"}) {
            assertThat("missing field [" + field + "]", meta.has(field), is(true));
        }

        JsonNode data = taxonList.get("objects");
        Iterator<JsonNode> elements = data.getElements();
        while (elements.hasNext()) {
            assertValidTaxon(elements.next());
        }

    }

    @Test
    public void getTaxon() throws IOException {
        String uri = getURLPrefix() + "mangal/taxa/1?format=json";
        String response = HttpClient.httpGet(uri);
        assertThat(response, is(notNullValue()));
    }

    protected void assertValidTaxon(JsonNode taxonObj) {
        String[] optionalFields = new String[]{"bold", "description", "eol", "gbif", "id", "itis", "ncbi", "vernacular"};
        for (String optionalField : optionalFields) {
            assertThat("missing field [" + optionalField + "]", taxonObj.has(optionalField), is(true));
        }

        String[] mandatoryFields = new String[]{"name", "owner", "status"};
        for (String mandatoryField : mandatoryFields) {
            assertThat("missing value for field [" + mandatoryField + "]", taxonObj.get(mandatoryField), is(notNullValue()));
        }

        assertThat(taxonObj.get("traits").isArray(), is(true));
    }

}
