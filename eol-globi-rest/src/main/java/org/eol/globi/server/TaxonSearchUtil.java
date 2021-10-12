package org.eol.globi.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.springframework.web.bind.annotation.PathVariable;
import sun.misc.Request;

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

    public interface LinkMapper {
        String linkForNode(JsonNode node);
    }

    public static Collection<String> linksForTaxonName(@PathVariable("taxonPath") String taxonPath,
                                                       HttpServletRequest request,
                                                       LinkMapper linkMapper) throws IOException {

        final CypherQuery pagedQuery = createPagedQuery(taxonPath, request);

        final String response = CypherUtil.executeRemote(pagedQuery);
        JsonNode rowsAndMetas = RequestHelper.getRowsAndMetas(response);
        Collection<String> links = new HashSet<>();
        if (rowsAndMetas != null) {
            for (JsonNode rowAndMeta : rowsAndMetas) {
                addLinksFromNode(links, RequestHelper.getRow(rowAndMeta), linkMapper);
            }
        }
        return links;
    }

    public static void addLinksFromNode(Collection<String> links, JsonNode jsonNode, LinkMapper linkMapper) {
        links.add(linkMapper.linkForNode(jsonNode));
    }

    public static CypherQuery createPagedQuery(String taxonPath, HttpServletRequest request) {
        Map parameterMap = request == null ? null : request.getParameterMap();
        final CypherQuery query1 = getCypherQuery(taxonPath, parameterMap);
        return CypherQueryBuilder.createPagedQuery(request, query1, 30);
    }

    public static CypherQuery getCypherQuery(String taxonPath, Map parameterMap) {
        final String pathQuery = CypherQueryBuilder.lucenePathQuery(Collections.singletonList(taxonPath), true);
        StringBuilder query = new StringBuilder("START someTaxon = node:taxons({pathQuery}) ");

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
        if (parameterMap != null) {
            requestedFields.addAll(CypherQueryBuilder.collectRequestedFields(parameterMap));
        }

        query.append(" MATCH someTaxon-[:SAME_AS*0..1]->taxon WHERE exists(taxon.externalId) WITH DISTINCT(taxon.externalId) as externalId, taxon.externalUrl as externalUrl ");
        CypherReturnClauseBuilder.appendReturnClauseDistinctz(
                query,
                CypherReturnClauseBuilder.actualReturnFields(requestedFields, Arrays.asList(returnFieldsCloseMatches), selectors.keySet()),
                selectors);
        return new CypherQuery(query.toString(), new HashMap<String, String>() {
            {
                put("pathQuery", pathQuery);
            }
        }, CypherUtil.CYPHER_VERSION_2_3);
    }
}
