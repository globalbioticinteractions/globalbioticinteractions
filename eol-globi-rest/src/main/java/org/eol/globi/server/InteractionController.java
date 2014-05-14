package org.eol.globi.server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
public class InteractionController {

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getInteractionTypes() throws IOException {
        return "{ \"" + CypherQueryBuilder.INTERACTION_PREYS_ON + "\":{\"source\":\"predator\",\"target\":\"prey\"}" +
                ",\"" + CypherQueryBuilder.INTERACTION_PREYED_UPON_BY + "\":{\"source\":\"prey\",\"target\":\"predator\"}" +
                ",\"" + CypherQueryBuilder.INTERACTION_PARASITE_OF + "\":{\"source\":\"parasite\",\"target\":\"host\"}" +
                ",\"" + CypherQueryBuilder.INTERACTION_HOST_OF + "\":{\"source\":\"host\",\"target\":\"parasite\"}" + "}";
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        Map parameterMap = request.getParameterMap();
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(parameterMap);
        return new CypherQueryExecutor(query.getQuery(), query.getParams()).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public String findPreyOf(HttpServletRequest request,
                             @PathVariable("sourceTaxonName") String sourceTaxonName,
                             @PathVariable("interactionType") String interactionType) throws IOException {
        Map parameterMap = request == null ? null : request.getParameterMap();
        CypherQueryExecutor executor;
        if (shouldIncludeObservations(request, parameterMap)) {
            executor = findObservationsForInteraction(sourceTaxonName, interactionType, null, parameterMap);
        } else {
            executor = findDistinctTaxonInteractions(sourceTaxonName, interactionType, null, parameterMap);
        }

        return executor.execute(request);
    }


    public CypherQueryExecutor findDistinctTaxonInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.distinctInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        return new CypherQueryExecutor(cypherQuery);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType,
                                     @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        CypherQueryExecutor result;
        Map parameterMap = request == null ? null : request.getParameterMap();

        if (shouldIncludeObservations(request, parameterMap)) {
            result = findObservationsForInteraction(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        } else {
            result = findDistinctTaxonInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        }

        return result.execute(request);
    }

    private boolean shouldIncludeObservations(HttpServletRequest request, Map parameterMap) {
        String includeObservations = parameterMap == null ? null : request.getParameter("includeObservations");
        return "true".equalsIgnoreCase(includeObservations);
    }

    private CypherQueryExecutor findObservationsForInteraction(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.interactionObservations(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        return new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams());
    }


    public String findObservationsOf(HttpServletRequest request,
                                     String sourceTaxonName,
                                     String interactionType) throws IOException {
        return findObservationsOf(request, sourceTaxonName, interactionType, null);
    }

}
