package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.junit.matchers.JUnitMatchers.hasItem;

/**
 * Note that Animal Diversity Web data tested below is currently assumed to be part of SPIRE.
 */

public class StudyImporterForVertNetTest extends GraphDBTestCase {


    @Test
    public void testResponse() throws IOException {
        List<String> stomachStrings = new ArrayList<String>();
        JsonNode jsonNode = parseResponse(stomachStrings, IOUtils.toString(new GZIPInputStream(getClass().getResourceAsStream("vertnet/example_response.json.gz"))));
        assertThat(jsonNode, is(notNullValue()));
        assertThat(stomachStrings, hasItems("gravel", "insect parts", "sand"));
    }

    @Ignore
    @Test
    public void importStomachData() throws URISyntaxException, IOException {
        String oldCursor = null;
        String cursor = null;


        while (StringUtils.isBlank(cursor) || !StringUtils.equals(cursor, oldCursor)) {
            List<String> stomachStrings = new ArrayList<String>();
            String uri = createRequestURL(cursor);
            HttpResponse resp = HttpUtil.createHttpClient().execute(new HttpGet(uri));

            if (200 == resp.getStatusLine().getStatusCode()) {
                String jsonString = IOUtils.toString(resp.getEntity().getContent());
                JsonNode jsonNode = parseResponse(stomachStrings, jsonString);
                if (jsonNode.has("cursor")) {
                    oldCursor = cursor;
                    cursor = jsonNode.get("cursor").getTextValue();
                }
            }
        }
    }

    private JsonNode parseResponse(List<String> stomachStrings, String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonString);
        JsonNode recs = jsonNode.get("recs");

        for (JsonNode rec : recs) {
            parseRecord(stomachStrings, rec);
        }
        return jsonNode;
    }


    private void parseRecord(Collection<String> stomachStrings, JsonNode rec) throws IOException {
        if (rec.has("dynamicproperties")) {
            String props = rec.get("dynamicproperties").getTextValue();
            Map<String, String> dynProps = new HashMap<String, String>();
            String[] split = props.split(";");
            if (split.length > 1) {
                for (String s1 : split) {
                    String nameValue[] = s1.split("[:=]");
                    if (nameValue.length > 1) {
                        dynProps.put(StringUtils.lowerCase(StringUtils.trim(nameValue[0])),
                                StringUtils.trim(nameValue[1]));
                    }
                }

            }
            if (dynProps.containsKey("stomach contents")) {
                String stomachContents = dynProps.get("stomach contents");
                String remarks = "";
                stomachStrings.add(stomachContents);
                if ("contents recorded".equals(stomachContents)) {
                    if (rec.has("occurrenceremarks")) {
                        remarks = rec.get("occurrenceremarks").getTextValue();
                    }
                }
                System.out.println("\"" + rec.get("individualid").getTextValue() + "\",\"" + stomachContents + "\", \"" + remarks + "\"");
            }
        }
    }

    private String createRequestURL(String cursor) {
        String s = "http://api.vertnet-portal.appspot.com/api/search?q=%7B%22q%22%3A%22stomach%22%2C%22l%22%3A%2210%22";
        if (cursor != null) {
            s += "%2C%22c%22%3A%22" + cursor + "%22";
        }
        return s + "%7D";
    }


}
