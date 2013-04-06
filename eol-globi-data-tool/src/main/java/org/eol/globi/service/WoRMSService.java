package org.eol.globi.service;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class WoRMSService extends BaseExternalIdService  {
    public static final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";
    public static final String RESPONSE_SUFFIX = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    public static final String URN_LSID_PREFIX = "urn:lsid:marinespecies.org:taxname:";

    public String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException {
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
        requestBody = requestBody + "<scientificname>" + taxonName + "</scientificname>";
        requestBody = requestBody + "<marine_only>false</marine_only>";
        requestBody += "</getAphiaID></soap:Body></soap:Envelope>";

        InputStreamEntity catchEntity = null;
        try {
            catchEntity = new InputStreamEntity(new ByteArrayInputStream(requestBody.getBytes("UTF-8")), requestBody.getBytes().length);
        } catch (UnsupportedEncodingException e) {
            throw new TaxonPropertyLookupServiceException("problem creating request body for [" + post.getURI().toString() + "]", e);
        }
        post.setEntity(catchEntity);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = getHttpClient().execute(post, responseHandler);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to connect to [" + post.getURI().toString() + "]", e);
        }

        String lsid = null;
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(RESPONSE_SUFFIX)) {
            String trimmed = response.replace(RESPONSE_PREFIX, "");
            trimmed = trimmed.replace(RESPONSE_SUFFIX, "");
            try {
                Long aphiaId = Long.parseLong(trimmed);
                lsid = URN_LSID_PREFIX + aphiaId;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        return lsid;
    }

}
