package org.eol.globi.service;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvoServiceImpl2 extends BaseHttpClientService implements EnvoService2 {
    private static final Log LOG = LogFactory.getLog(EnvoServiceImpl2.class);

    private Map<String, List<EnvoTerm2>> spireLookup = null;

    private static final List<EnvoTerm2> EMPTY_LIST = new ArrayList<EnvoTerm2>();

    @Override
    public List<EnvoTerm2> lookupBySPIREHabitat(String name) throws EnvoServiceException {
        String uri = "http://purl.obolibrary.org/obo/envo/mappings/spire-mapping.txt";
        if (spireLookup == null) {
            buildMapping(uri);
        }
        List<EnvoTerm2> envoTerms = spireLookup.get(name);
        return envoTerms == null ? EMPTY_LIST : envoTerms;
    }

    private void buildMapping(String uri) throws EnvoServiceException {
        spireLookup = new HashMap<String, List<EnvoTerm2>>();
        LOG.info("ENVO data populating with [" + uri + "]...");
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = execute(get, responseHandler);
            CSVParser parser = new CSVParser(new StringReader(response));
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                String spireName = line[1];
                String envoId = line[2];
                String envoName = line[3];
                if (StringUtils.isNotBlank(spireName)
                        && StringUtils.isNotBlank(envoId)
                        && StringUtils.isNotBlank(envoName)) {
                    List<EnvoTerm2> envoTerms = spireLookup.get(spireName);
                    if (envoTerms == null) {
                        envoTerms = new ArrayList<EnvoTerm2>();
                        spireLookup.put(spireName, envoTerms);
                    }
                    envoTerms.add(new EnvoTerm2(envoId, envoName));
                }
            }
            LOG.info("ENVO data populated.");
        } catch (IOException ex) {
            throw new EnvoServiceException("cannot map ENVO to SPIRE term: failed to retrieve or parse [" + uri + "]", ex);
        }
    }
}
