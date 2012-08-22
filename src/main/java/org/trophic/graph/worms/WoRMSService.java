package org.trophic.graph.worms;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class WoRMSService {


    public static final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";
    private HttpClient httpClient;
    public static final String RESPONSE_SUFFIX = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    public WoRMSService() {
        this.httpClient = new DefaultHttpClient();
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public String lookupLSIDByTaxonName(String scientificName) throws IOException {
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

        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";

        String lsid = null;
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(RESPONSE_SUFFIX)) {
            String trimmed = response.replace(RESPONSE_PREFIX, "");
            trimmed = trimmed.replace(RESPONSE_SUFFIX, "");
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
