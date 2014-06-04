package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SearchService {
    private static final Log LOG = LogFactory.getLog(SearchService.class);

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
            props.put("name", first.get(0).getTextValue());
            props.put("commonNames", first.get(1).getTextValue());
            props.put("path", first.get(2).getTextValue());
            props.put("externalId", first.get(3).getTextValue());
        }
        return props;
    }
    public String findTaxonProxy(@PathVariable("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        CypherQuery cypherQuery = new CypherQuery("START taxon = node:taxons(name={taxonName}) " +
                "RETURN taxon.name? as `name`, taxon.commonNames? as `commonNames`, taxon.path? as `path`, taxon.externalId? as `externalId` LIMIT 1", new HashMap<String, String>() {
            {
                put("taxonName", taxonName);
            }
        });
        return new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(request, false);
    }

    @RequestMapping(value = "/findCloseMatchesForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String findCloseMatchesForCommonAndScientificNames(@PathVariable("taxonName") final String taxonName) throws IOException {
        String luceneQuery = buildLuceneQuery(taxonName, "name");
        CypherQuery cypherQuery = new CypherQuery("START taxon = node:taxonNameSuggestions('" + luceneQuery + "') " +
                "RETURN taxon.name? as `taxon.name`, taxon.commonNames? as `taxon.commonNames`, taxon.path? as `taxon.path` LIMIT 15", null);
        return new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams()).execute(null, false);
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
