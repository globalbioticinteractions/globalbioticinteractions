package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.eol.globi.util.ExternalIdUtil;
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
    public CypherQuery locationsNew(HttpServletRequest request) throws IOException {
        return CypherQueryBuilder.locations();
    }

    @RequestMapping(value = "/findExternalUrlForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForTaxonWithName(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        return getUrlFromExternalId(CypherUtil.executeRemote(findExternalIdForTaxonNew(request, taxonName)));
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
        return getUrlFromExternalId(CypherUtil.executeRemote(findExternalIdForStudyNew(request, taxonName)));
    }

    @RequestMapping(value = "/findExternalIdForTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery findExternalIdForTaxonNew(HttpServletRequest request, @PathVariable("taxonName") final String taxonName) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.externalIdForTaxon(taxonName);
        return CypherQueryBuilder.createPagedQuery(request, cypherQuery);
    }

    @RequestMapping(value = "/findExternalIdForStudy/{studyTitle}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery findExternalIdForStudyNew(HttpServletRequest request, @PathVariable("studyTitle") final String studyTitle) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.externalIdForStudy(studyTitle);
        return CypherQueryBuilder.createPagedQuery(request, cypherQuery);
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
    public CypherQuery findShortestPathsNew(HttpServletRequest request, @PathVariable("startTaxon") final String startTaxon,
                                            @PathVariable("endTaxon") final String endTaxon) throws IOException {
        CypherQuery cypherQuery = CypherQueryBuilder.shortestPathQuery(startTaxon, endTaxon);
        return CypherQueryBuilder.createPagedQuery(request, cypherQuery);
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