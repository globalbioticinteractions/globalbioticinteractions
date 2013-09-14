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

public class ENVOServiceImpl extends BaseHttpClientService implements ENVOService {
    private static final Log LOG = LogFactory.getLog(ENVOServiceImpl.class);

    private Map<String, List<ENVOTerm>> spireLookup = null;

    private static final List<ENVOTerm> EMPTY_LIST = new ArrayList<ENVOTerm>();

    @Override
    public List<ENVOTerm> lookupBySPIREHabitat(String name) throws ENVOServiceException {
        String uri = "http://envo.googlecode.com/svn/trunk/src/envo/mappings/spire-mapping.txt";
        if (spireLookup == null) {
            buildMapping(uri);
        }
        List<ENVOTerm> envoTerms = spireLookup.get(name);
        return envoTerms == null ? EMPTY_LIST : envoTerms;
    }

    private void buildMapping(String uri) throws ENVOServiceException {
        spireLookup = new HashMap<String, List<ENVOTerm>>();
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
                    List<ENVOTerm> envoTerms = spireLookup.get(spireName);
                    if (envoTerms == null) {
                        envoTerms = new ArrayList<ENVOTerm>();
                        spireLookup.put(spireName, envoTerms);
                    }
                    envoTerms.add(new ENVOTerm(envoId, envoName));
                }
            }
            LOG.info("ENVO data populated.");
        } catch (IOException ex) {
            throw new ENVOServiceException("cannot map ENVO to SPIRE term: failed to retrieve or parse [" + uri + "]", ex);
        }
    }
}
