package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.eol.globi.util.ExternalIdUtil;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        return toMap(findTaxonProxy(taxonName));
    }

    public Map<String, String> toMap(String response) throws IOException {
        JsonNode node = new ObjectMapper().readTree(response);
        JsonNode dataNode = node.get("data");
        Map<String, String> props = NO_PROPERTIES;

        if (dataNode != null && dataNode.size() > 0) {
            props = new HashMap<String, String>();
            JsonNode first = dataNode.get(0);
            props.put(PropertyAndValueDictionary.NAME, StringUtils.defaultString(first.get(0).getTextValue()));
            props.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.defaultString(first.get(1).getTextValue()));
            props.put(PropertyAndValueDictionary.PATH, StringUtils.defaultString(first.get(2).getTextValue()));
            final String externalId = StringUtils.defaultString(first.get(3).getTextValue());
            props.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);

            final String externalURL = StringUtils.defaultString(first.get(4).getTextValue());
            if (StringUtils.isNotBlank(externalId) && StringUtils.isBlank(externalURL)) {
                props.put(PropertyAndValueDictionary.EXTERNAL_URL, StringUtils.defaultString(ExternalIdUtil.urlForExternalId(externalId)));
            } else {
                props.put(PropertyAndValueDictionary.EXTERNAL_URL, externalURL);
            }

            props.put(PropertyAndValueDictionary.THUMBNAIL_URL, StringUtils.defaultString(first.get(5).getTextValue()));
        }
        return props;
    }

    public String findTaxonProxy(@PathVariable("taxonName") final String taxonName) throws IOException {
        CypherQuery cypherQuery = new CypherQuery(queryPrefix() +
                returnClause(), paramForName(taxonName));
        return CypherUtil.executeRemote(cypherQuery);
    }

    public HashMap<String, String> paramForName(@PathVariable("taxonName") final String taxonName) {
        return new HashMap<String, String>() {
            {
                put("taxonPathQuery", "path:\\\"" + taxonName + "\\\"");
                put("taxonName", taxonName);
            }
        };
    }

    public Map<String, String> findTaxonWithImage(final String taxonName) throws IOException {
        CypherQuery cypherQuery = new CypherQuery(queryPrefix()
                + " AND has(taxon." + PropertyAndValueDictionary.THUMBNAIL_URL + ") " +
                "AND length(taxon." + PropertyAndValueDictionary.THUMBNAIL_URL + ") > 0 " +
                returnClause(), paramForName(taxonName));
        return toMap(CypherUtil.executeRemote(cypherQuery));
    }

    @RequestMapping(value = "/findCloseMatches", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CypherQuery findCloseMatches(@RequestParam("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
        return findCloseMatchesForCommonAndScientificNames(taxonName, request);
    }

    @RequestMapping(value = "/findCloseMatchesForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public CypherQuery findCloseMatchesForCommonAndScientificNames(@PathVariable("taxonName") final String taxonName, HttpServletRequest request) throws IOException {
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
        CypherReturnClauseBuilder.appendReturnClauseDistinctz(query, CypherReturnClauseBuilder.actualReturnFields(requestedFields, Arrays.asList(returnFieldsCloseMatches), selectors.keySet()), selectors);
        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(query.toString(), null), 15);
    }

    @RequestMapping(value = "/taxonLinks/{taxonPath}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Collection<String> taxonLinks(@PathVariable("taxonPath") final String taxonPath, HttpServletRequest request) throws IOException {
        final String pathQuery = CypherQueryBuilder.lucenePathQuery(Collections.singletonList(taxonPath), true);
        StringBuilder query = new StringBuilder("START someTaxon = node:taxons({pathQuery}) ");

        Map<ResultField, String> selectors = new HashMap<ResultField, String>() {
            {
                put(ResultField.TAXON_EXTERNAL_ID, "externalId");
                put(ResultField.TAXON_EXTERNAL_URL, "externalUrl");
            }
        };

        ResultField[] returnFieldsCloseMatches = new ResultField[]{
                ResultField.TAXON_EXTERNAL_ID,
                ResultField.TAXON_EXTERNAL_URL
        };

        List<String> requestedFields = new ArrayList<String>();
        if (request != null) {
            requestedFields.addAll(CypherQueryBuilder.collectRequestedFields(request.getParameterMap()));
        }

        query.append(" MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE has(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl? as externalUrl ");
        CypherReturnClauseBuilder.appendReturnClauseDistinctz(query, CypherReturnClauseBuilder.actualReturnFields(requestedFields, Arrays.asList(returnFieldsCloseMatches), selectors.keySet()), selectors);
        final CypherQuery query1 = new CypherQuery(query.toString(), new HashMap() {
            {
                put("pathQuery", pathQuery);
            }
        });
        final CypherQuery pagedQuery = CypherQueryBuilder.createPagedQuery(request, query1, 15);
        final String response = CypherUtil.executeRemote(pagedQuery);
        JsonNode node = new ObjectMapper().readTree(response);
        JsonNode dataNode = node.get("data");
        Collection<String> links = new HashSet<>();
        if (dataNode != null) {
            for (JsonNode jsonNode : dataNode) {
                if (jsonNode.isArray() && jsonNode.size() > 1) {
                    String externalId = jsonNode.get(0).asText();
                    String externalUrl = jsonNode.get(1).asText();
                    String resolvedUrl = ExternalIdUtil.urlForExternalId(externalId);
                    if (StringUtils.isNotBlank(resolvedUrl)) {
                        links.add(resolvedUrl);
                    } else if (StringUtils.isNotBlank(externalUrl)) {
                        links.add(externalUrl);
                    }
                }
            }
        }
        return links;
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

    public String queryPrefix() {
        return "START taxon = node:taxonPaths({taxonPathQuery}) " +
                "MATCH taxon-[:SAME_AS*0..1]->otherTaxon\n" +
                "WHERE ((has(otherTaxon.name) AND otherTaxon.name = {taxonName})\n" +
                "OR (has(otherTaxon.externalId) AND otherTaxon.externalId = {taxonName}))\n";
    }

    private String returnClause() {
        return "RETURN taxon.name? as `" + PropertyAndValueDictionary.NAME + "`" +
                ", taxon.commonNames? as `" + PropertyAndValueDictionary.COMMON_NAMES + "`" +
                ", taxon.path? as `" + PropertyAndValueDictionary.PATH + "`" +
                ", taxon.externalId? as `" + PropertyAndValueDictionary.EXTERNAL_ID + "`" +
                ", taxon.externalUrl? as `" + PropertyAndValueDictionary.EXTERNAL_URL + "`" +
                ", taxon.thumbnailUrl? as `" + PropertyAndValueDictionary.THUMBNAIL_URL + "` LIMIT 1";
    }
}
