package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ReportController {

    @RequestMapping(value = "/reports/studies", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery studies(@RequestParam(required = false) final String source, final HttpServletRequest request) throws IOException {
        String cypherQuery = "START report = node:reports(" + (StringUtils.isBlank(source) ? "'source:*'" : "source={source}") + ") "
            + " WHERE has(report.title) "
            + " RETURN report.citation? as " + ResultField.STUDY_CITATION
            + ", report.externalId? as " + ResultField.STUDY_URL
            + ", report.doi? as " + ResultField.STUDY_DOI
            + ", report.source? as " + ResultField.STUDY_SOURCE_CITATION
            + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
            + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
            + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
            + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES
            + ", report.nTaxaNoMatch? as " + ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH;
        Map<String, String> params = StringUtils.isBlank(source) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(cypherQuery, params));
    }

    @RequestMapping(value = "/dataset", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery dataset(final HttpServletRequest request) throws IOException {
        return datasetQuery2(request, "namespace", null);
    }

    @RequestMapping(value = "/source", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sourceRoot(final HttpServletRequest request) throws IOException {
        return datasetQuery(request, "namespace", null);
    }

    @RequestMapping(value = "/source/{org}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sourceOrg(
        @PathVariable("org") final String org,
        final HttpServletRequest request) throws IOException {
        return datasetQuery(request, "namespace", org);
    }

    @RequestMapping(value = "/source/{org}/{name}", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sourceOrgName(
        @PathVariable("org") final String org,
        @PathVariable("name") final String name,
        final HttpServletRequest request) throws IOException {
        return datasetQuery(request, "namespace", org + "/" + name);
    }


    @RequestMapping(value = "/reports/sources", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery sources(@RequestParam(required = false) final String sourceId,
                               final HttpServletRequest request) throws IOException {
        return sourceQuery(request, sourceId);
    }

    private CypherQuery sourceQuery(HttpServletRequest request, final String sourceId) {
        String searchMatch = "sourceId" + "={sourceId}";
        if (StringUtils.isBlank(sourceId)) {
            searchMatch = "'" + "sourceId" + ":*'";
        }
        String cypherQuery = "START report = node:reports(" + searchMatch + ") "
            + " RETURN report.citation? as " + ResultField.STUDY_CITATION
            + ", report.externalId? as " + ResultField.STUDY_URL
            + ", report.doi? as " + ResultField.STUDY_DOI
            + ", report.source? as " + ResultField.STUDY_SOURCE_CITATION
            + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
            + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
            + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
            + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES
            + ", report.nTaxaNoMatch? as " + ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH
            + ", report.sourceId? as " + ResultField.STUDY_SOURCE_ID;

        String sourceIdActual = StringUtils.countMatches(sourceId, ":") > 0 ? sourceId : "globi:" + sourceId;
        Map<String, String> params = StringUtils.isBlank(sourceId) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("sourceId", sourceIdActual);
        }};

        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(cypherQuery, params));
    }

    private CypherQuery datasetQuery(HttpServletRequest request, String searchKey, final String searchValue) {
        String searchMatch = searchKey + "={namespace}";
        if (StringUtils.isBlank(searchValue)) {
            searchMatch = "'" + searchKey + ":*'";
        }
        String cypherQuery = "START dataset = node:datasets(" + searchMatch + "), report = node:reports('sourceId:*') "
            + " WHERE ('globi:' + dataset.namespace) = report.sourceId "
            + " RETURN report.citation? as " + ResultField.STUDY_CITATION
            + ", report.externalId? as " + ResultField.STUDY_URL
            + ", report.doi? as " + ResultField.STUDY_DOI
            + ", dataset.citation? as " + ResultField.STUDY_SOURCE_CITATION
            + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
            + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
            + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
            + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES
            + ", report.nTaxaNoMatch? as " + ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH
            + ", report.sourceId? as " + ResultField.STUDY_SOURCE_ID
            + ", dataset.doi? as " + ResultField.STUDY_SOURCE_DOI
            + ", dataset.format? as " + ResultField.STUDY_SOURCE_FORMAT
            + ", dataset.archiveURI? as " + ResultField.STUDY_SOURCE_ARCHIVE_URI
            + ", dataset.lastSeenAt? as " + ResultField.STUDY_SOURCE_LAST_SEEN_AT;


        Map<String, String> params = StringUtils.isBlank(searchValue) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("namespace", searchValue);
        }};

        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(cypherQuery, params));
    }

    private CypherQuery datasetQuery2(HttpServletRequest request, String searchKey, final String searchValue) {
        String searchMatch = searchKey + "={namespace}";
        if (StringUtils.isBlank(searchValue)) {
            searchMatch = "'" + searchKey + ":*'";
        }
        String cypherQuery = "START dataset = node:datasets(" + searchMatch + ") "
            + " RETURN null as " + ResultField.STUDY_CITATION
            + ", null as " + ResultField.STUDY_URL
            + ", null as " + ResultField.STUDY_DOI
            + ", null as " + ResultField.STUDY_SOURCE_CITATION
            + ", null as " + ResultField.NUMBER_OF_INTERACTIONS
            + ", null as " + ResultField.NUMBER_OF_DISTINCT_TAXA
            + ", null as " + ResultField.NUMBER_OF_STUDIES
            + ", null as " + ResultField.NUMBER_OF_SOURCES
            + ", null as " + ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH
            + ", 'globi:' + dataset.namespace as " + ResultField.STUDY_SOURCE_ID
            + ", dataset.doi? as " + ResultField.STUDY_SOURCE_DOI
            + ", dataset.format? as " + ResultField.STUDY_SOURCE_FORMAT
            + ", dataset.archiveURI? as " + ResultField.STUDY_SOURCE_ARCHIVE_URI
            + ", dataset.lastSeenAt? as " + ResultField.STUDY_SOURCE_LAST_SEEN_AT;


        Map<String, String> params = StringUtils.isBlank(searchValue) ? CypherQueryBuilder.EMPTY_PARAMS : new HashMap<String, String>() {{
            put("namespace", searchValue);
        }};

        return CypherQueryBuilder.createPagedQuery(request, new CypherQuery(cypherQuery, params));
    }

    @RequestMapping(value = "/reports/collections", method = RequestMethod.GET)
    @ResponseBody
    public CypherQuery collections() throws IOException {
        String cypherQuery = "START report = node:reports('collection:*')" +
            " WHERE not(has(report.title)) AND not(has(report.source)) "
            + " RETURN report.citation? as " + ResultField.STUDY_CITATION
            + ", report.externalId? as " + ResultField.STUDY_URL
            + ", report.doi? as " + ResultField.STUDY_DOI
            + ", report.source? as " + ResultField.STUDY_SOURCE_CITATION
            + ", report.nInteractions as " + ResultField.NUMBER_OF_INTERACTIONS
            + ", report.nTaxa as " + ResultField.NUMBER_OF_DISTINCT_TAXA
            + ", report.nStudies? as " + ResultField.NUMBER_OF_STUDIES
            + ", report.nSources? as " + ResultField.NUMBER_OF_SOURCES
            + ", report.nTaxaNoMatch? as " + ResultField.NUMBER_OF_DISTINCT_TAXA_NO_MATCH;
        return new CypherQuery(cypherQuery);
    }

}
