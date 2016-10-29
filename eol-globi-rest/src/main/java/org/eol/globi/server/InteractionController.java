package org.eol.globi.server;

import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class InteractionController {

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    protected CypherQuery findInteractionsNew(HttpServletRequest request) {
        Map parameterMap = request.getParameterMap();
        CypherQueryBuilder.QueryType queryType = CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT;

        if (shouldIncludeObservations(request, parameterMap)) {
            queryType = CypherQueryBuilder.QueryType.MULTI_TAXON_ALL;
        } else if (isTaxonQueryOnly(parameterMap)) {
            queryType = CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY;
        }
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
        String includeObservations = parameterMap == null ? null : request.getParameter(ParamName.INCLUDE_OBSERVATIONS.getName());
        return "t".equalsIgnoreCase(includeObservations) || "true".equalsIgnoreCase(includeObservations);
    }

    private boolean isTaxonQueryOnly(Map parameterMap) {
        List<String> accordingTo = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        List<String> bbox = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.BBOX);
        return accordingTo.isEmpty() && bbox.isEmpty();
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
