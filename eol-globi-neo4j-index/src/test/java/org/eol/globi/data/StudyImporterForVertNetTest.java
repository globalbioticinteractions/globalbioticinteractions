package org.eol.globi.data;

import com.Ostermiller.util.CSVPrint;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

/**
 * Note that Animal Diversity Web data tested below is currently assumed to be part of SPIRE.
 */

public class StudyImporterForVertNetTest extends GraphDBTestCase {

    private static final Log LOG = LogFactory.getLog(StudyImporterForVertNetTest.class);

    @Test
    public void testResponse() throws IOException {
        List<String> stomachStrings = new ArrayList<String>();
        File file = File.createTempFile("vertnet", ".csv");
        file.deleteOnExit();
        JsonNode jsonNode = parseResponse(stomachStrings, IOUtils.toString(new GZIPInputStream(getClass().getResourceAsStream("vertnet/example_response.json.gz"))),
                new FileOutputStream(file));
        assertThat(jsonNode, is(notNullValue()));
        assertThat(stomachStrings, hasItems("gravel", "insect parts", "sand"));
    }

    @Test
    public void parseAssociatedOccurrences() {
        Map<String, InteractType> assoc = parseAssOcc("(eaten by) institutional catalog number CUMV Rept 4988");
        assertThat(assoc.get("http://arctos.database.museum/guid/CUMV:Rept:4988"), is(InteractType.EATEN_BY));

        assoc = parseAssOcc("(ate) DMNS:Mamm http://arctos.database.museum/guid/DMNS:Mamm:13142; (ate) DMNS:Mamm http://arctos.database.museum/guid/DMNS:Mamm:13143");
        assertThat(assoc.get("http://arctos.database.museum/guid/DMNS:Mamm:13142"), is(InteractType.ATE));
        assertThat(assoc.get("http://arctos.database.museum/guid/DMNS:Mamm:13143"), is(InteractType.ATE));
    }

    protected Map<String, InteractType> parseAssOcc(String str) {
        Map<String, InteractType> mapping = new TreeMap<String, InteractType>() {
            {
                put("ate", InteractType.ATE);
                put("eaten by", InteractType.EATEN_BY);
                put("parasite of", InteractType.PARASITE_OF);
            }
        };

        String[] occurrences = StringUtils.split(str, ";");
        Map<String, InteractType> assoc = new TreeMap<String, InteractType>();
        for (String occurrence : occurrences) {
            int index = StringUtils.indexOf(occurrence, ")");
            if (index > 0) {
                String association = StringUtils.substring(StringUtils.trim(StringUtils.substring(occurrence, 0, index)), 1);
                String rest = StringUtils.substring(occurrence, index + 1);
                InteractType interactType = mapping.get(association);
                if (interactType != null) {
                    String trim = StringUtils.trim(rest);
                    int nsIndex = StringUtils.indexOf(trim, "http://arctos.database.museum/guid/");
                    String strip = nsIndex < 0 ? trim : StringUtils.substring(trim, nsIndex, trim.length());
                    String ns = StringUtils.replace(strip, "institutional catalog number ", "http://arctos.database.museum/guid/");
                    assoc.put(StringUtils.replace(ns, " ", ":"), interactType);
                }
            }
        }
        return assoc;
    }

    @Ignore
    @Test
    public void parseAssociatedOccurrences2() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode1 = mapper.readTree(IOUtils.toString(new GZIPInputStream(getClass().getResourceAsStream("vertnet/response_associated_occurrences.json.gz"))));
        JsonNode recs = jsonNode1.get("recs");

        StringWriter linkOs = new StringWriter();
        CSVPrint linkPrinter = CSVTSVUtil.createCSVPrint(linkOs);
        linkPrinter.print(new String[]{"source", "interaction_type", "target"});
        linkPrinter.setAutoFlush(true);

        StringWriter nodeOs = new StringWriter();
        CSVPrint nodePrinter = CSVTSVUtil.createCSVPrint(nodeOs);
        String[] nodeFields = {"individualid", "decimallongitude", "decimallatitude"
                , "year", "month", "day", "basisofrecord", "scientificname"
                , "dataset_citation"};
        nodePrinter.print(nodeFields);
        nodePrinter.setAutoFlush(true);

        for (JsonNode rec : recs) {
            String specimenId = getOrNull(rec, "individualid");
            String associatedOcc = getOrNull(rec, "associatedoccurrences");
            if (StringUtils.isNotBlank(specimenId) && StringUtils.isNotBlank(associatedOcc)) {
                Map<String, InteractType> associatedoccurrences = parseAssOcc(associatedOcc);

                for (Map.Entry<String, InteractType> entry : associatedoccurrences.entrySet()) {
                    linkPrinter.println();
                    linkPrinter.print(specimenId);
                    linkPrinter.print(entry.getValue().toString());
                    linkPrinter.print(entry.getKey());
                }

                nodePrinter.println();
                for (String field : nodeFields) {
                    String value = getOrNull(rec, field);
                    nodePrinter.print(StringUtils.isBlank(value) ? "" : value);
                }
            }

        }
        linkPrinter.flush();
        linkPrinter.close();

        assertThat(linkOs.toString(), is("source,interaction_type,target" +
                "\nhttp://arctos.database.museum/guid/CUMV:Amph:16004,EATEN_BY,http://arctos.database.museum/guid/CUMV:Rept:4988" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34623,ATE,http://arctos.database.museum/guid/DMNS:Mamm:13142" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34623,ATE,http://arctos.database.museum/guid/DMNS:Mamm:13143" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34728,ATE,DZTM::Denver:Zoology:Tissue:Mammal:2285" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34728,ATE,http://arctos.database.museum/guid/DMNS:Mamm:13685"));

        assertThat(nodeOs.toString(), is("individualid,decimallongitude,decimallatitude,year,month,day,basisofrecord,scientificname,dataset_citation" +
                "\nindividualID,180,-90,2014,10,10,basisOfRecord,scientificName,test citation" +
                "\nhttp://arctos.database.museum/guid/CUMV:Amph:16004,-76.45442,42.4566,1953,09,27,PreservedSpecimen,Rana sylvatica," +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:21686,-106.434158,38.709448,1940,07,27,PreservedSpecimen,Lagopus leucurus,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:21688,-106.434158,38.709448,1940,07,27,PreservedSpecimen,Lagopus leucurus,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:21689,-106.434158,38.709448,1940,07,27,PreservedSpecimen,Lagopus leucurus,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:26722,-105.02222,39.38528,1943,06,24,PreservedSpecimen,Chordeiles minor,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:26724,-105.02222,39.38528,1943,06,24,PreservedSpecimen,Chordeiles minor,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34623,-104.738965,39.461082,2012,04,30,PreservedSpecimen,Asio otus,Denver Museum of Nature & Science Bird Collection" +
                "\nhttp://arctos.database.museum/guid/DMNS:Bird:34728,-105.096498,40.454918,2012,08,10,PreservedSpecimen,Buteo swainsoni,Denver Museum of Nature & Science Bird Collection"));
    }

    private String getOrNull(JsonNode node, String propertyName) {
        String value = null;
        if (node.has(propertyName)) {
            value = node.get(propertyName).asText();
        }
        return value;
    }

    @Test
    public void testRequestURL() throws IOException, URISyntaxException {
        assertThat(createRequestURL(null, "stomach").toString(), is("http://api.vertnet-portal.appspot.com:80/api/search?q=%7B%22l%22:100,%22q%22:%22stomach%22%7D"));
        assertThat(createRequestURL(null, "associatedOccurrences", 10).toString(), is("http://api.vertnet-portal.appspot.com:80/api/search?q=%7B%22l%22:10,%22q%22:%22associatedOccurrences%22%7D"));
    }

    @Test
    public void parseSingleRecord() {

    }

    @Ignore
    @Test
    public void importStomachData() throws URISyntaxException, IOException {
        String oldCursor = null;
        String cursor = null;
        File file = File.createTempFile("vertnet", ".csv");
        file.deleteOnExit();
        LOG.info("writing to: [" + file.getAbsolutePath() + "]");
        FileOutputStream fos = new FileOutputStream(file);
        while (StringUtils.isBlank(cursor) || !StringUtils.equals(cursor, oldCursor)) {
            List<String> stomachStrings = new ArrayList<String>();
            URI uri = createRequestURL(cursor, "stomach");
            HttpResponse resp = HttpUtil.getHttpClient().execute(new HttpGet(uri));

            if (200 == resp.getStatusLine().getStatusCode()) {
                String jsonString = IOUtils.toString(resp.getEntity().getContent());
                JsonNode jsonNode = parseResponse(stomachStrings, jsonString, fos);
                if (jsonNode.has("cursor")) {
                    oldCursor = cursor;
                    cursor = jsonNode.get("cursor").getTextValue();
                }
            }
            fos.flush();
        }
        fos.close();
    }

    private JsonNode parseResponse(List<String> stomachStrings, String jsonString, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonString);
        JsonNode recs = jsonNode.get("recs");

        for (JsonNode rec : recs) {
            parseRecord(stomachStrings, rec, outputStream);
        }
        return jsonNode;
    }


    private void parseRecord(Collection<String> stomachStrings, JsonNode rec, OutputStream outputStream) throws IOException {
        if (rec.has("dynamicproperties")) {
            String props = rec.get("dynamicproperties").getTextValue();
            Map<String, String> dynProps = new TreeMap<String, String>();
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
            String stomachContents = "";
            if (dynProps.containsKey("stomach contents")) {
                stomachContents = dynProps.get("stomach contents");
            }
            String remarks = "";
            stomachStrings.add(stomachContents);
            if (StringUtils.isNotBlank(stomachContents)) {
                if (rec.has("occurrenceremarks")) {
                    remarks = rec.get("occurrenceremarks").getTextValue();
                }
                String line = "\"" + (rec.has("individualid") ? rec.get("individualid").getTextValue() : "") + "\",\"" + stomachContents + "\", \"" + remarks + "\"\n";
                IOUtils.write(line, outputStream);
            }
        }


    }

    private URI createRequestURL(String cursor, String searchTerm) throws IOException, URISyntaxException {
        return createRequestURL(cursor, searchTerm, 100);
    }

    private URI createRequestURL(String cursor, String searchTerm, int limit) throws IOException, URISyntaxException {
        Map<String, Object> param = new TreeMap<String, Object>();
        param.put("q", searchTerm);
        param.put("l", limit);
        if (StringUtils.isNotBlank(cursor)) {
            param.put("c", cursor);
        }

        ObjectMapper mapper = new ObjectMapper();
        String query = mapper.writeValueAsString(param);
        return new URI("http", null, "api.vertnet-portal.appspot.com", 80, "/api/search", "q=" + query, null);
    }


}
