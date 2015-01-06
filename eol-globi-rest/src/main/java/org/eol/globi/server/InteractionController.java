package org.eol.globi.server;

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
        Map parameterMap = request.getParameterMap();
        CypherQueryBuilder.QueryType queryType = shouldIncludeObservations(request, parameterMap)
                        ? CypherQueryBuilder.QueryType.MULTI_TAXON_ALL
                        : CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT;
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(parameterMap, queryType);
        return new CypherQueryExecutor(query.getQuery(), query.getParams()).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public String findInteractions(HttpServletRequest request,
                                   @PathVariable("sourceTaxonName") String sourceTaxonName,
                                   @PathVariable("interactionType") String interactionType) throws IOException {
        return findInteractions(request, sourceTaxonName, interactionType, null);
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
        CypherQueryBuilder.QueryType queryType = shouldIncludeObservations(request, parameterMap)
                ? CypherQueryBuilder.QueryType.SINGLE_TAXON_ALL
                : CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT;
        result = createQueryExecutor(sourceTaxonName, interactionType, targetTaxonName, parameterMap, queryType);
        return result.execute(request);
    }

    private boolean shouldIncludeObservations(HttpServletRequest request, Map parameterMap) {
        String includeObservations = parameterMap == null ? null : request.getParameter("includeObservations");
        return "true".equalsIgnoreCase(includeObservations);
    }

    public static CypherQueryExecutor createQueryExecutor(final String sourceTaxonName, String interactionType, final String targetTaxonName, Map parameterMap, CypherQueryBuilder.QueryType queryType) throws IOException {
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
        CypherQuery cypherQuery = CypherQueryBuilder.buildInteractionQuery(sourceTaxa, interactionType, targetTaxa, parameterMap, queryType);
        return new CypherQueryExecutor(cypherQuery.getQuery(), cypherQuery.getParams());
    }
}
