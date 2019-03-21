package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TaxonSearchUtil {
    public static Collection<String> linksForTaxonName(@PathVariable("taxonPath") String taxonPath, HttpServletRequest request) throws IOException {
        final String pathQuery = CypherQueryBuilder.lucenePathQuery(Collections.singletonList(taxonPath), true);
        StringBuilder query = new StringBuilder("CYPHER 1.9 START someTaxon = node:taxons({pathQuery}) ");

        Map<ResultField, String> selectors = new HashMap<ResultField, String>() {
            {
                put(ResultField.TAXON_EXTERNAL_ID, "externalId");
                put(ResultField.TAXON_EXTERNAL_URL, "externalUrl");
            }
        };

        ResultField[] returnFieldsCloseMatches = new ResultField[]{
                ResultField.TAXON_EXTERNAL_ID,
                ResultField.TAXON_EXTERNAL_URL
        };

        List<String> requestedFields = new ArrayList<>();
        if (request != null) {
            requestedFields.addAll(CypherQueryBuilder.collectRequestedFields(request.getParameterMap()));
        }

        query.append(" MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE has(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl? as externalUrl ");
        CypherReturnClauseBuilder.appendReturnClauseDistinctz(query, CypherReturnClauseBuilder.actualReturnFields(requestedFields, Arrays.asList(returnFieldsCloseMatches), selectors.keySet()), selectors);
        final CypherQuery query1 = new CypherQuery(query.toString(), new HashMap<String, String>() {
            {
                put("pathQuery", pathQuery);
            }
        });
        final CypherQuery pagedQuery = CypherQueryBuilder.createPagedQuery(request, query1, 30);
        final String response = CypherUtil.executeRemote(pagedQuery);
        JsonNode node = new ObjectMapper().readTree(response);
        JsonNode dataNode = node.get("data");
        Collection<String> links = new HashSet<>();
        if (dataNode != null) {
            for (JsonNode jsonNode : dataNode) {
                if (jsonNode.isArray() && jsonNode.size() > 1) {
                    String externalId = jsonNode.get(0).asText();
                    String externalUrl = jsonNode.get(1).asText();
                    String resolvedUrl = ExternalIdUtil.urlForExternalId(externalId);
                    if (StringUtils.isNotBlank(resolvedUrl)) {
                        links.add(resolvedUrl);
                    } else if (StringUtils.isNotBlank(externalUrl)) {
                        links.add(externalUrl);
                    }
                }
            }
        }
        return links;
    }
}
