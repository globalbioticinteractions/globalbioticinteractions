package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WoRMSService implements PropertyEnricher {
    public static final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";
    public static final String RESPONSE_SUFFIX = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    private final LanguageCodeLookup languageLookup;

    public WoRMSService() {
        languageLookup = new LanguageCodeLookup();
    }

    public String lookupIdByName(String taxonName) throws PropertyEnricherException {
        String response = getResponse("getAphiaID", "scientificname", taxonName);
        String id = null;
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(RESPONSE_SUFFIX)) {
            String trimmed = response.replace(RESPONSE_PREFIX, "");
            trimmed = trimmed.replace(RESPONSE_SUFFIX, "");
            try {
                Long aphiaId = Long.parseLong(trimmed);
                id = TaxonomyProvider.ID_PREFIX_WORMS + aphiaId;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        return id;
    }

    private String getResponse(String methodName, String paramName, String paramValue) throws PropertyEnricherException {
        HttpPost post = new HttpPost("http://www.marinespecies.org/aphia.php?p=soap");
        post.setHeader("SOAPAction", "http://tempuri.org/getAphiaID");
        post.setHeader("Content-Type", "text/xml;charset=utf-8");
        String requestBody = "<?xml version=\"1.0\" ?>";
        requestBody += "<soap:Envelope ";
        requestBody += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
        requestBody += "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ";
        requestBody += "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";
        requestBody += "<soap:Body>";
        requestBody += "<" + methodName + " xmlns=\"http://tempuri.org/\">";
        requestBody = requestBody + "<" + paramName + ">" + paramValue + "</" + paramName + ">";
        requestBody = requestBody + "<marine_only>false</marine_only>";
        requestBody += "</" + methodName + "></soap:Body></soap:Envelope>";

        InputStreamEntity catchEntity;
        try {
            catchEntity = new InputStreamEntity(new ByteArrayInputStream(requestBody.getBytes("UTF-8")), requestBody.getBytes().length);
        } catch (UnsupportedEncodingException e) {
            throw new PropertyEnricherException("problem creating request body for [" + post.getURI().toString() + "]", e);
        }
        post.setEntity(catchEntity);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpUtil.executeWithTimer(post, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to connect to [" + post.getURI().toString() + "]", e);
        }
        return response;
    }

    public Map<String, String> enrichById(final String id, final Map<String, String> properties) throws PropertyEnricherException {
        if (isAlphiaID(id)) {
            String response = getResponse("getAphiaRecordByID", "AphiaID", id.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
            String aphiaId = id;
            String validAphiaId = XmlUtil.extractName(response, "valid_AphiaID");
            if (StringUtils.isNotBlank(validAphiaId)) {
                aphiaId = TaxonomyProvider.ID_PREFIX_WORMS + validAphiaId;
                properties.put(PropertyAndValueDictionary.EXTERNAL_ID, aphiaId);
                properties.put(PropertyAndValueDictionary.NAME, XmlUtil.extractName(response, "valid_name"));
            }
            if (isAlphiaID(aphiaId)) {
                String response1 = getResponse("getAphiaClassificationByID", "AphiaID", aphiaId.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
                String value = XmlUtil.extractPath(response1, "scientificname", "");
                properties.put(PropertyAndValueDictionary.PATH, StringUtils.isBlank(value) ? null : value);
                value = XmlUtil.extractPath(response1, "AphiaID", TaxonomyProvider.ID_PREFIX_WORMS);
                properties.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.isBlank(value) ? null : value);
                value = XmlUtil.extractPath(response1, "rank", "");
                String[] ranks = StringUtils.splitPreserveAllTokens(value, CharsetConstant.SEPARATOR);
                if (ranks != null && ranks.length > 0) {
                    properties.put(PropertyAndValueDictionary.RANK, StringUtils.trim(StringUtils.lowerCase(ranks[ranks.length - 1])));
                }

                properties.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.isBlank(value) ? null : StringUtils.lowerCase(value));

                response1 = getResponse("getAphiaVernacularsByID", "AphiaID", aphiaId.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
                String vernaculars = XmlUtil.extractPath(response1, "vernacular", "");
                String languageCodes = XmlUtil.extractPath(response1, "language_code", "");
                String[] commonNames = StringUtils.splitByWholeSeparator(vernaculars, CharsetConstant.SEPARATOR);
                String[] langCodes = StringUtils.splitByWholeSeparator(languageCodes, CharsetConstant.SEPARATOR);
                List<String> names = new ArrayList<String>();
                for (int i = 0; i < commonNames.length && (langCodes.length == commonNames.length); i++) {
                    String code = languageLookup.lookupLanguageCodeFor(langCodes[i]);
                    names.add(commonNames[i] + " @" + (code == null ? langCodes[i] : code));
                }
                properties.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(names, CharsetConstant.SEPARATOR));
            }
        }

        return properties;
    }

    protected String lookupTaxonPathById(String id) throws PropertyEnricherException {
        String path = null;
        if (isAlphiaID(id)) {
            String response = getResponse("getAphiaClassificationByID", "AphiaID", id.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
            path = XmlUtil.extractPath(response, "scientificname", "");
        }
        return StringUtils.isBlank(path) ? null : path;
    }

    protected boolean isAlphiaID(String id) {
        return StringUtils.startsWith(id, TaxonomyProvider.ID_PREFIX_WORMS);
    }

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        if (!isAlphiaID(properties.get(PropertyAndValueDictionary.EXTERNAL_ID))) {
            enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, lookupIdByName(properties.get(PropertyAndValueDictionary.NAME)));
        }

        enrichedProperties = enrichById(enrichedProperties.get(PropertyAndValueDictionary.EXTERNAL_ID), enrichedProperties);

        return Collections.unmodifiableMap(enrichedProperties);
    }

    @Override
    public void shutdown() {

    }
}
