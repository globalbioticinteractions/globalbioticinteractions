package org.eol.globi.service;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvoServiceImpl extends BaseHttpClientService implements EnvoService {
    private static final Log LOG = LogFactory.getLog(EnvoServiceImpl.class);

    private Map<String, List<EnvoTerm>> mapping = null;

    private static final List<EnvoTerm> EMPTY_LIST = Collections.emptyList();
    private String mappingURI = "http://purl.obolibrary.org/obo/envo/mappings/spire-mapping.txt";

    @Override
    public List<EnvoTerm> lookupTermByName(String name) throws EnvoServiceException {
        if (mapping == null) {
            buildMapping(getMappingURI());
        }
        List<EnvoTerm> envoTerms = mapping.get(name);
        return envoTerms == null ? EMPTY_LIST : envoTerms;
    }

    private void buildMapping(String uri) throws EnvoServiceException {
        mapping = new HashMap<String, List<EnvoTerm>>();
        LOG.info("term mapping populating with [" + uri + "]...");
        try {
            String response = IOUtils.toString(new URI(uri));
            CSVParser parser = new CSVParser(new StringReader(response));
            parser.changeDelimiter('\t');
            String[] line;
            while ((line = parser.getLine()) != null) {
                if (line.length < 4) {
                    LOG.info("line: [" + parser.getLastLineNumber() + "] in [" + uri + "] contains less than 4 columns");
                } else {
                    String spireName = line[1];
                    String envoId = line[2];
                    String envoName = line[3];
                    if (StringUtils.isNotBlank(spireName)
                            && StringUtils.isNotBlank(envoId)
                            && StringUtils.isNotBlank(envoName)) {
                        List<EnvoTerm> envoTerms = mapping.get(spireName);
                        if (envoTerms == null) {
                            envoTerms = new ArrayList<EnvoTerm>();
                            mapping.put(spireName, envoTerms);
                        }
                        envoTerms.add(new EnvoTerm(envoId, envoName));
                    }
                }
            }
            LOG.info("term mapping populated.");
        } catch (IOException e) {
            throw new EnvoServiceException("failed to retrieve mapping from [" + uri + "]", e);
        } catch (URISyntaxException e) {
            throw new EnvoServiceException("failed to retrieve mapping from [" + uri + "]", e);
        }
    }

    public void setMappingURI(String mappingURI) {
        this.mappingURI = mappingURI;
    }

    protected String getMappingURI() {
        return mappingURI;
    }

}
