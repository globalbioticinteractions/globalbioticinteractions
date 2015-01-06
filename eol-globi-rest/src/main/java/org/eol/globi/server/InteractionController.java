package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
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

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET)
    @ResponseBody
    public String getInteractionTypes(HttpServletRequest request) throws IOException {
        String type = request == null ? "json" : request.getParameter("type");
        String result;
        if ("csv".equals(type)) {
            StringBuilder builder = new StringBuilder();
            builder.append("interaction,source,target\n");
            builder.append(CypherQueryBuilder.INTERACTION_PREYS_ON).append(",predator,prey\n");
            builder.append(CypherQueryBuilder.INTERACTION_PREYED_UPON_BY).append(",prey,predator\n");
            builder.append(CypherQueryBuilder.INTERACTION_PARASITE_OF).append(",parasite,host\n");
            builder.append(CypherQueryBuilder.INTERACTION_HOST_OF).append(",host,parasite\n");
            builder.append(CypherQueryBuilder.INTERACTION_POLLINATES).append(",pollinator,plant\n");
            builder.append(CypherQueryBuilder.INTERACTION_POLLINATED_BY).append(",plant,pollinator\n");
            builder.append(CypherQueryBuilder.INTERACTION_PATHOGEN_OF).append(",pathogen,host\n");
            builder.append(CypherQueryBuilder.INTERACTION_HAS_PATHOGEN).append(",plant,pollinator");
            builder.append(CypherQueryBuilder.INTERACTION_SYMBIONT_OF).append(",source,target");
            builder.append(CypherQueryBuilder.INTERACTION_INTERACTS_WITH).append(",source,target");
            result = builder.toString();
        } else {
            result = "{ \"" + CypherQueryBuilder.INTERACTION_PREYS_ON + "\":{\"source\":\"predator\",\"target\":\"prey\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_PREYED_UPON_BY + "\":{\"source\":\"prey\",\"target\":\"predator\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_PARASITE_OF + "\":{\"source\":\"parasite\",\"target\":\"host\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_HOST_OF + "\":{\"source\":\"host\",\"target\":\"parasite\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_POLLINATES + "\":{\"source\":\"pollinator\",\"target\":\"plant\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_POLLINATED_BY + "\":{\"source\":\"plant\",\"target\":\"pollinator\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_PATHOGEN_OF + "\":{\"source\":\"pathogen\",\"target\":\"host\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_HAS_PATHOGEN + "\":{\"source\":\"host\",\"target\":\"pathogen\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_SYMBIONT_OF + "\":{\"source\":\"source\",\"target\":\"target\"}" +
                    ",\"" + CypherQueryBuilder.INTERACTION_INTERACTS_WITH + "\":{\"source\":\"source\",\"target\":\"target\"}" +
                    "}";
        }
        return result;
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(request.getParameterMap());
        return new CypherQueryExecutor(query.getQuery(), query.getParams()).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public String findInteractions(HttpServletRequest request,
                                   @PathVariable("sourceTaxonName") String sourceTaxonName,
                                   @PathVariable("interactionType") String interactionType) throws IOException {
        return findInteractions(request, sourceTaxonName, interactionType, null);
    }


    public CypherQueryExecutor findDistinctTaxonInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.distinctInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        return new CypherQueryExecutor(cypherQuery);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request,
                                   @PathVariable("sourceTaxonName") String sourceTaxonName,
                                   @PathVariable("interactionType") String interactionType,
                                   @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        CypherQueryExecutor result;
        Map parameterMap = request == null ? null : request.getParameterMap();

        if (CypherQueryBuilder.shouldIncludeObservations(parameterMap)) {
            result = findObservationsForInteraction(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        } else {
            result = findDistinctTaxonInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        }

        return result.execute(request);
    }

    private CypherQueryExecutor findObservationsForInteraction(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.interactionObservations(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        return new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams());
    }
}
