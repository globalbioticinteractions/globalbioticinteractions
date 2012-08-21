package org.trophic.graph.worms;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class WormsTest {

    @Test
    public void lookupExistingSpeciesTaxon() throws IOException {
        String lsid = lookupLSID("Peprilus burti");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:276560"));
    }

    @Test
    public void lookupExistingGenusTaxon() throws IOException {
        String lsid = lookupLSID("Peprilus");
        assertThat(lsid, is("urn:lsid:marinespecies.org:taxname:159825"));
    }

    @Test
    public void lookupNonExistentTaxon() throws IOException {
        String lsid = lookupLSID("Brutus blahblahi");
        assertThat(lsid, is(nullValue()));

    }

    private String lookupLSID(String scientificName) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://www.marinespecies.org/aphia.php?p=soap");
        post.setHeader("SOAPAction", "http://tempuri.org/getAphiaID");
        post.setHeader("Content-Type", "text/xml;charset=utf-8");

        String requestBody = "<?xml version=\"1.0\" ?>";
        requestBody += "<soap:Envelope ";
        requestBody += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
        requestBody += "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ";
        requestBody += "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";
        requestBody += "<soap:Body>";
        requestBody += "<getAphiaID xmlns=\"http://tempuri.org/\">";
        requestBody = requestBody + "<scientificname>" + scientificName + "</scientificname>";
        requestBody = requestBody + "<marine_only>false</marine_only>";
        requestBody += "</getAphiaID></soap:Body></soap:Envelope>";

        InputStreamEntity catchEntity = new InputStreamEntity(new ByteArrayInputStream(requestBody.getBytes("UTF-8")), requestBody.getBytes().length);
        post.setEntity(catchEntity);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response = httpClient.execute(post, responseHandler);
        httpClient.getConnectionManager().shutdown();

        String suffix = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";

        String lsid = null;
        if (response.startsWith(prefix) && response.endsWith(suffix)) {
            String trimmed = response.replace(prefix, "");
            trimmed = trimmed.replace(suffix, "");
            try {
                Long aphiaId = Long.parseLong(trimmed);
                lsid = "urn:lsid:marinespecies.org:taxname:" + aphiaId;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        return lsid;
    }
}
