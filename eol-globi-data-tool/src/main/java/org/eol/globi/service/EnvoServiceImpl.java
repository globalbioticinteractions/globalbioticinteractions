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

public class EnvoServiceImpl extends BaseHttpClientService implements EnvoService {
    private static final Log LOG = LogFactory.getLog(EnvoServiceImpl.class);

    private Map<String, List<EnvoTerm>> spireLookup = null;

    private static final List<EnvoTerm> EMPTY_LIST = new ArrayList<EnvoTerm>();

    @Override
    public List<EnvoTerm> lookupBySPIREHabitat(String name) throws EnvoServiceException {
        String uri = "http://purl.obolibrary.org/obo/envo/mappings/spire-mapping.txt";
        if (spireLookup == null) {
            buildMapping(uri);
        }
        List<EnvoTerm> envoTerms = spireLookup.get(name);
        return envoTerms == null ? EMPTY_LIST : envoTerms;
    }

    private void buildMapping(String uri) throws EnvoServiceException {
        spireLookup = new HashMap<String, List<EnvoTerm>>();
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
                    List<EnvoTerm> envoTerms = spireLookup.get(spireName);
                    if (envoTerms == null) {
                        envoTerms = new ArrayList<EnvoTerm>();
                        spireLookup.put(spireName, envoTerms);
                    }
                    envoTerms.add(new EnvoTerm(envoId, envoName));
                }
            }
            LOG.info("ENVO data populated.");
        } catch (IOException ex) {
            throw new EnvoServiceException("cannot map ENVO to SPIRE term: failed to retrieve or parse [" + uri + "]", ex);
        }
    }
}
