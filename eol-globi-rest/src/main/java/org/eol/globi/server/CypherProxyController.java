package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
public class CypherProxyController {

    @Autowired
    private GraphDatabaseService graphDb;

    private final CypherQueryBuilder cypherQueryBuilder = new CypherQueryBuilder();

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getInteractionTypes() throws IOException {
        return "{ \"" + CypherQueryBuilder.INTERACTION_PREYS_ON + "\":{\"source\":\"predator\",\"target\":\"prey\"}" +
                ",\"" + CypherQueryBuilder.INTERACTION_PREYED_UPON_BY + "\":{\"source\":\"prey\",\"target\":\"predator\"}}";
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        Map parameterMap = request.getParameterMap();
        CypherQuery query = cypherQueryBuilder.buildInteractionQuery(parameterMap);
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

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findTaxon(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.findTaxon(taxonName);
        return new CypherQueryExecutor(cypherQuery).execute(request);
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

    @RequestMapping(value = "/locations", method = RequestMethod.GET)
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations(HttpServletRequest request) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.locations()).execute(request);
    }

    @RequestMapping(value = "/contributors", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors(@RequestParam(required = false) final String source) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.references(source)).execute(null);
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "infoCache")
    public String info(@RequestParam(required = false) final String source) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.stats(source)).execute(null);
    }

    @RequestMapping(value = "/sources", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "sourcesCache")
    public String sources() throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.sourcesQuery()).execute(null);
    }


    @RequestMapping(value = "/findExternalUrlForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForTaxonWithName(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        String result = findExternalIdForTaxon(request, taxonName);
        return getUrlFromExternalId(result);
    }

    private String getUrlFromExternalId(String result) {
        String urlString = null;
        for (Map.Entry<String, String> stringStringEntry : ExternalIdUtil.getURLPrefixMap().entrySet()) {
            urlString = getUrl(result, stringStringEntry.getKey(), stringStringEntry.getValue());
            if (urlString != null && urlString.startsWith("http")) {
                break;
            }

        }
        return buildJsonUrl(urlString);
    }

    @RequestMapping(value = "/findExternalUrlForStudy/{studyTitle}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForStudyWithTitle(HttpServletRequest request, @PathVariable("studyTitle") String taxonName) throws IOException {
        String result = findExternalIdForStudy(request, taxonName);
        return getUrlFromExternalId(result);
    }

    @RequestMapping(value = "/findExternalIdForTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findExternalIdForTaxon(HttpServletRequest request, @PathVariable("taxonName") final String taxonName) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.externalIdForTaxon(taxonName);
        return new CypherQueryExecutor(cypherQuery).execute(request);
    }

    @RequestMapping(value = "/findExternalIdForStudy/{studyTitle}", method = RequestMethod.GET)
    @ResponseBody
    public String findExternalIdForStudy(HttpServletRequest request, @PathVariable("studyTitle") final String studyTitle) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.externalIdForStudy(studyTitle);
        return new CypherQueryExecutor(cypherQuery).execute(request);
    }

    private String buildJsonUrl(String url) {
        return StringUtils.isBlank(url) ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    @RequestMapping(value = "/findExternalUrlForExternalId/{externalId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForExternalId(@PathVariable("externalId") String externalId) {
        return buildJsonUrl(ExternalIdUtil.infoURLForExternalId(externalId));
    }

    @RequestMapping(value = "/shortestPathsBetweenTaxon/{startTaxon}/andTaxon/{endTaxon}", method = RequestMethod.GET)
    @ResponseBody
    public String findShortestPaths(HttpServletRequest request, @PathVariable("startTaxon") final String startTaxon,
                                    @PathVariable("endTaxon") final String endTaxon) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.shortestPathQuery(startTaxon, endTaxon);

        return new CypherQueryExecutor(cypherQuery).execute(request);
    }

    private String getUrl(String result, String externalIdPrefix, String urlPrefix) {
        String url = "";
        if (result.contains(externalIdPrefix)) {
            String[] split = result.split(externalIdPrefix);
            if (split.length > 1) {
                String[] externalIdParts = split[1].split("\"");
                if (externalIdParts.length > 1) {
                    url = urlPrefix + externalIdParts[0];
                }
            }
        }
        return url;
    }


}