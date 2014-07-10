package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonomyProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
public class MangalController {

    public static final String MANGAL_TAXA_PATH = "/mangal/taxa/";
    public static final String TAXON_QUERY_MATCH_CLAUSE = "MATCH taxon-[:SAME_AS]->otherTaxon RETURN id(taxon) as id, taxon.name as name, taxon.commonNames? as commonNames, collect(otherTaxon.externalId) as externalIds, taxon.externalId as externalId ";

    @RequestMapping(value = MANGAL_TAXA_PATH, method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    @ResponseBody
    public Map<String, Object> listTaxa(@RequestParam(value = "offset", required = false, defaultValue = "0") final Long offset
            , @RequestParam(value = "limit", required = false, defaultValue = "20") final Long limit) throws IOException {

        String countQuery = "START taxon = node:taxons('*:*') return count(taxon.name) as count";
        String countResult = new CypherQueryExecutor(countQuery, null).execute(null, false);

        final Long count = new ObjectMapper().readTree(countResult).get("data").get(0).get(0).asLong();


        Map<String, Object> response = new HashMap<String, Object>();
        response.put("meta", new HashMap<String, Object>() {
            {
                put("limit", limit);
                put("offset", offset);
                put("next", offset >= count ? null : nextPath(offset, limit));
                put("previous", offset == 0 ? null : previousPath(offset, limit));
                put("total_count", count);
            }
        });

        String startClauseAllTaxa = "START taxon = node:taxons('*:*') ";
        String taxonQueryLimit = "SKIP " + offset + " LIMIT " + limit;
        String taxonQuery = startClauseAllTaxa + TAXON_QUERY_MATCH_CLAUSE + taxonQueryLimit;
        String resultJson = new CypherQueryExecutor(taxonQuery, null).execute(null, false);
        List<Map<String, Object>> objects = new ArrayList<Map<String, Object>>();
        JsonNode jsonNode = new ObjectMapper().readTree(resultJson);
        Iterator<JsonNode> elements = jsonNode.get("data").getElements();
        while (elements.hasNext()) {
            Map<String, Object> taxon = parseTaxonRow(elements.next());
            objects.add(taxon);
        }
        response.put("objects", objects);

        return response;
    }

    protected Map<String, Object> parseTaxonRow(JsonNode row) {
        Map<String, Object> taxon = new HashMap<String, Object>();
        taxon.put("id", row.get(0).asText());
        taxon.put("status", "confirmed");
        taxon.put("description", null);
        taxon.put("owner", "GloBI");
        taxon.put("vernacular", null);
        taxon.put("name", row.get(1).asText());
        taxon.put("ncbi", null);
        taxon.put("gbif", null);
        taxon.put("eol", null);
        taxon.put("itis", null);
        taxon.put("bold", null);
        taxon.put("traits", new ArrayList<String>());

        String commonNames = row.get(2).asText();
        String[] commonNameVariations = StringUtils.split(commonNames, "|");
        for (String commonNameVariant : commonNameVariations) {
            if (StringUtils.contains(commonNameVariant, "@en")) {
                taxon.put("vernacular", commonNameVariant.replace("@en", "").trim());
            }
        }
        Iterator<JsonNode> externalIds = row.get(3).getElements();
        while (externalIds.hasNext()) {
            String externalId = externalIds.next().asText();
            mapExternalId(taxon, externalId);
        }
        mapExternalId(taxon, row.get(4).asText());

        return taxon;
    }

    private void mapExternalId(Map<String, Object> taxon, String externalId) {
        if (externalId.startsWith(TaxonomyProvider.GBIF.getIdPrefix())) {
            taxon.put("gbif", StringUtils.replace(externalId, TaxonomyProvider.GBIF.getIdPrefix(), ""));
        } else if (externalId.startsWith(TaxonomyProvider.NCBI.getIdPrefix())) {
            taxon.put("ncbi", StringUtils.replace(externalId, TaxonomyProvider.NCBI.getIdPrefix(), ""));
        } else if (externalId.startsWith(TaxonomyProvider.EOL.getIdPrefix())) {
            taxon.put("eol", StringUtils.replace(externalId, TaxonomyProvider.EOL.getIdPrefix(), ""));
        } else if (externalId.startsWith(TaxonomyProvider.ITIS.getIdPrefix())) {
            taxon.put("itis", StringUtils.replace(externalId, TaxonomyProvider.ITIS.getIdPrefix(), ""));
        }
    }

    private String nextPath(Long offset, Long limit) {
        return MANGAL_TAXA_PATH + "?offset=" + (offset + limit) + "&limit=" + limit + "&format=json";
    }

    private String previousPath(Long offset, Long limit) {
        Long previousOffset = offset < limit ? 0 : offset - limit;
        return MANGAL_TAXA_PATH + "?offset=" + previousOffset + "&limit=" + limit + "&format=json";
    }

    @RequestMapping(value = "/mangal/taxa/{taxonId}", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    @ResponseBody
    public Map<String, Object> findTaxon(@PathVariable("taxonId") Long taxonId) throws IOException {
        String taxonJson = new CypherQueryExecutor("START taxon = node(" + taxonId + ") " + TAXON_QUERY_MATCH_CLAUSE, null).execute(null);
        JsonNode data = new ObjectMapper().readTree(taxonJson).get("data");
        return data.size() == 0 ? new HashMap<String, Object>() : parseTaxonRow(data.get(0));
    }

}