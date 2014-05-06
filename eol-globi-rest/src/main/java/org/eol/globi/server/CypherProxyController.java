package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
public class CypherProxyController {

    @RequestMapping(value = "/locations", method = RequestMethod.GET)
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations(HttpServletRequest request) throws IOException {
        return new CypherQueryExecutor(CypherQueryBuilder.locations()).execute(request, false);
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