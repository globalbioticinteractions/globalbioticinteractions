package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.server.util.InteractionTypeExternal;
import org.eol.globi.server.util.ResultFormatterCSV;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Controller
public class InteractionController {

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET)
    @ResponseBody
    public String getInteractionTypes(HttpServletRequest request) throws IOException {
        Collection<InteractionTypeExternal> availableTypes = Arrays.asList(InteractionTypeExternal.values());
        if (request != null) {
            if (StringUtils.isNotBlank(request.getParameter(CypherQueryBuilder.TAXON_HTTP_PARAM_NAME))) {
                CypherQuery cypherQuery = CypherQueryBuilder.buildInteractionTypeQuery(request.getParameterMap());
                String interactionTypes = new ResultFormatterCSV().format(CypherUtil.executeRemote(cypherQuery));
                System.out.println(interactionTypes);
                String[] interactionType = StringUtils.replace(interactionTypes, "\"", "").split("\n");
                availableTypes = new HashSet<InteractionTypeExternal>();
                for (String type : interactionType) {
                    InteractionTypeExternal value = CypherQueryBuilder.INTERACTION_TYPE_INTERNAL_EXTERNAL_MAP.get(type);
                    if (value != null) {
                        availableTypes.add(value);
                    }
                }
            }
        }
        String type = request == null ? "json" : request.getParameter("type");
        return "csv".equals(type) ? interactionMapCsv(availableTypes) : interactionMapJson(availableTypes);
    }

    protected String interactionMapJson(Collection<InteractionTypeExternal> availableTypes) {
        List<String> interactions = new ArrayList<String>();
        for (InteractionTypeExternal value : availableTypes) {
            interactions.add("\"" + value.getLabel() + "\":"
                    + "{\"source\":\"" + value.getSource()
                    + "\"," + "\"target\":\"" + value.getTarget() + "\"}");
        }
        return "{" + StringUtils.join(interactions, ",") + "}";
    }

    protected String interactionMapCsv(Collection<InteractionTypeExternal> availableTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("interaction,source,target\n");
        for (InteractionTypeExternal value : availableTypes) {
            builder.append(value.getLabel());
            builder.append(",").append(value.getSource());
            builder.append(",").append(value.getTarget()).append("\n");
        }
        return builder.toString();
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    protected CypherQuery findInteractionsNew(HttpServletRequest request) {
        Map parameterMap = request.getParameterMap();
        CypherQueryBuilder.QueryType queryType = shouldIncludeObservations(request, parameterMap)
                ? CypherQueryBuilder.QueryType.MULTI_TAXON_ALL
                : CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT;
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(parameterMap, queryType);
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    @RequestMapping(value = "/taxon", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public CypherQuery findDistinctTaxa(HttpServletRequest request) throws IOException {
        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery((Map<String, String[]>) request.getParameterMap());
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public CypherQuery findInteractionsNew(HttpServletRequest request,
                                           @PathVariable("sourceTaxonName") String sourceTaxonName,
                                           @PathVariable("interactionType") String interactionType) throws IOException {
        return findInteractionsNew(request, sourceTaxonName, interactionType, null);
    }


    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery findInteractionsNew(HttpServletRequest request,
                                           @PathVariable("sourceTaxonName") String sourceTaxonName,
                                           @PathVariable("interactionType") String interactionType,
                                           @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        Map parameterMap = request == null ? null : request.getParameterMap();
        CypherQueryBuilder.QueryType queryType = shouldIncludeObservations(request, parameterMap)
                ? CypherQueryBuilder.QueryType.SINGLE_TAXON_ALL
                : CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT;
        CypherQuery query = createQuery(sourceTaxonName, interactionType, targetTaxonName, parameterMap, queryType);
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    private boolean shouldIncludeObservations(HttpServletRequest request, Map parameterMap) {
        String includeObservations = parameterMap == null ? null : request.getParameter("includeObservations");
        return "t".equalsIgnoreCase(includeObservations) || "true".equalsIgnoreCase(includeObservations);
    }

    public static CypherQuery createQuery(final String sourceTaxonName, String interactionType, final String targetTaxonName, Map parameterMap, CypherQueryBuilder.QueryType queryType) throws IOException {
        List<String> sourceTaxa = new ArrayList<String>() {{
            if (sourceTaxonName != null) {
                add(sourceTaxonName);
            }
        }};
        List<String> targetTaxa = new ArrayList<String>() {{
            if (targetTaxonName != null) {
                add(targetTaxonName);
            }
        }};
        return CypherQueryBuilder.buildInteractionQuery(sourceTaxa, interactionType, targetTaxa, parameterMap, queryType);
    }
}
