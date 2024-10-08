package org.eol.globi.server;

import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final static Logger LOG = LoggerFactory.getLogger(InteractionController.class);


    @RequestMapping(value = "/exists", method = {RequestMethod.GET, RequestMethod.HEAD})
    @ResponseBody
    protected String atLeastOneInteraction(HttpServletRequest request) throws IOException {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/401
        Map parameterMap = getParamMap(request);
        CypherQuery query = CypherQueryBuilder
                .buildInteractionQuery(
                        parameterMap,
                        QueryType.forParams(parameterMap)
                );

        CypherQuery pagedQuery = CypherQueryBuilder.createPagedQuery(query, 0, 1);
        String s = CypherUtil.executeRemote(pagedQuery);

        if (RequestHelper.emptyData(s)) {
            throw new ResourceNotFoundException("no results for query with params: " + parameterMap);
        }
        return "OK";
    }

    @RequestMapping(value = "/interaction", method = {RequestMethod.GET})
    @ResponseBody
    protected CypherQuery findInteractions(HttpServletRequest request) {
        Map parameterMap = getParamMap(request);
        CypherQuery query = CypherQueryBuilder.buildInteractionQuery(parameterMap, QueryType.forParams(parameterMap));
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    private static Map getParamMap(HttpServletRequest request) {
        return request.getParameterMap();
    }

    @RequestMapping(value = "/taxon", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public CypherQuery findDistinctTaxa(HttpServletRequest request) throws IOException {
        CypherQuery query = CypherQueryBuilder.createDistinctTaxaInLocationQuery((Map<String, String[]>) getParamMap(request));
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public CypherQuery findInteractions(HttpServletRequest request,
                                        @PathVariable("sourceTaxonName") String sourceTaxonName,
                                        @PathVariable("interactionType") String interactionType) throws IOException {
        return findInteractions(request, sourceTaxonName, interactionType, null);
    }


    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery findInteractions(HttpServletRequest request,
                                        @PathVariable("sourceTaxonName") String sourceTaxonName,
                                        @PathVariable("interactionType") String interactionType,
                                        @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        Map parameterMap = request == null ? null : getParamMap(request);
        CypherQuery query = createQuery(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        return CypherQueryBuilder.createPagedQuery(request, query);
    }

    public static CypherQuery createQuery(final String sourceTaxonName, String interactionType, final String targetTaxonName, Map parameterMap) throws IOException {
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
        QueryType queryType = QueryType.forParamsSingle(parameterMap);
        return CypherQueryBuilder.buildInteractionQuery(sourceTaxa, interactionType, targetTaxa, parameterMap, queryType);
    }
}
