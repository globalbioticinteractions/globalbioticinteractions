package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eol.globi.server.util.ResultField.TAXON_COMMON_NAMES;
import static org.eol.globi.server.util.ResultField.TAXON_EXTERNAL_ID;
import static org.eol.globi.server.util.ResultField.TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TAXON_PATH;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_IDS;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_RANKS;

@Controller
public class TaxonSearchImpl implements TaxonSearch {
    private static final Log LOG = LogFactory.getLog(TaxonSearchImpl.class);

    public static final HashMap<String, String> NO_PROPERTIES = new HashMap<String, String>();

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, String> findTaxon(@PathVariable("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        String response = findTaxonProxy(taxonName, request);
        JsonNode node = new ObjectMapper().readTree(response);
        JsonNode dataNode = node.get("data");
        Map<String, String> props = NO_PROPERTIES;

        if (dataNode != null && dataNode.size() > 0) {
            props = new HashMap<String, String>();
            JsonNode first = dataNode.get(0);
            props.put(PropertyAndValueDictionary.NAME, valueOrEmpty(first.get(0).getTextValue()));
            props.put(PropertyAndValueDictionary.COMMON_NAMES, valueOrEmpty(first.get(1).getTextValue()));
            props.put(PropertyAndValueDictionary.PATH, valueOrEmpty(first.get(2).getTextValue()));
            props.put(PropertyAndValueDictionary.EXTERNAL_ID, valueOrEmpty(first.get(3).getTextValue()));
        }
        return props;
    }

    protected String valueOrEmpty(String name) {
        return StringUtils.isBlank(name) ? "" : name;
    }

    public String findTaxonProxy(@PathVariable("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        CypherQuery cypherQuery = new CypherQuery("START taxon = node:taxons(name={taxonName}) " +
                "RETURN taxon.name? as `name`, taxon.commonNames? as `commonNames`, taxon.path? as `path`, taxon.externalId? as `externalId` LIMIT 1", new HashMap<String, String>() {
            {
                put("taxonName", taxonName);
            }
        });
        return CypherUtil.executeRemote(cypherQuery);
    }

    @RequestMapping(value = "/findCloseMatches", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CypherQuery findCloseMatches(@RequestParam("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        return findCloseMatchesForCommonAndScientificNamesNew(taxonName, request);
    }

    @RequestMapping(value = "/findCloseMatchesForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CypherQuery findCloseMatchesForCommonAndScientificNamesNew(@PathVariable("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        String luceneQuery = buildLuceneQuery(taxonName, "name");
        StringBuilder query = new StringBuilder("START taxon = node:taxonNameSuggestions('" + luceneQuery + "') ");

        Map<ResultField, String> selectors = new HashMap<ResultField, String>() {
            {
                put(TAXON_NAME, "taxon.name?");
                put(TAXON_COMMON_NAMES, "taxon.commonNames?");
                put(TAXON_PATH, "taxon.path?");
                put(TAXON_EXTERNAL_ID, "taxon.externalId?");
                put(TAXON_PATH_IDS, "taxon.pathIds?");
                put(TAXON_PATH_RANKS, "taxon.pathNames?");
            }
        };

        ResultField[] returnFieldsCloseMatches = new ResultField[]{TAXON_NAME,
                TAXON_COMMON_NAMES,
                TAXON_PATH,
                TAXON_PATH_IDS
        };

        List<String> requestedFields = new ArrayList<String>();
        if (request != null) {
            requestedFields.addAll(CypherQueryBuilder.collectRequestedFields(request.getParameterMap()));
        }
        CypherQueryBuilder.appendReturnClausez(query, CypherQueryBuilder.actualReturnFields(requestedFields, Arrays.asList(returnFieldsCloseMatches), selectors.keySet()), selectors);
        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(query.toString(), null), 15);
    }

    private String buildLuceneQuery(String taxonName, String name) {
        StringBuilder builder = new StringBuilder();
        String[] split = StringUtils.split(taxonName, " ");
        for (int i = 0; i < split.length; i++) {
            builder.append("(");
            builder.append(name);
            builder.append(":");
            String part = split[i];
            builder.append(part.toLowerCase());
            builder.append("* OR ");
            builder.append(name);
            builder.append(":");
            builder.append(part.toLowerCase());
            builder.append("~)");
            if (i < (split.length - 1)) {
                builder.append(" AND ");
            }
        }
        String queryString = builder.toString();
        LOG.info("query: [" + queryString + "]");
        return queryString;
    }
}
