package org.eol.globi.service;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Term;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TermLookupServiceImpl extends BaseHttpClientService implements TermLookupService {
    private static final Log LOG = LogFactory.getLog(TermLookupServiceImpl.class);

    private Map<String, List<Term>> mapping = null;

    public abstract String getMappingURI();

    @Override
    public List<Term> lookupTermByName(final String name) throws TermLookupServiceException {
        if (mapping == null) {
            buildMapping(getMappingURI());
        }
        List<Term> terms = mapping.get(name);
        return terms == null ? new ArrayList<Term>() {{
            add(new Term(PropertyAndValueDictionary.NO_MATCH, name));
        }} : terms;
    }

    private void buildMapping(String uri) throws TermLookupServiceException {
        mapping = new HashMap<String, List<Term>>();
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
                        List<Term> terms = mapping.get(spireName);
                        if (terms == null) {
                            terms = new ArrayList<Term>();
                            mapping.put(spireName, terms);
                        }
                        terms.add(new Term(envoId, envoName));
                    }
                }
            }
            LOG.info("term mapping populated.");
        } catch (IOException e) {
            throw new TermLookupServiceException("failed to retrieve mapping from [" + uri + "]", e);
        } catch (URISyntaxException e) {
            throw new TermLookupServiceException("failed to retrieve mapping from [" + uri + "]", e);
        }
    }

}
