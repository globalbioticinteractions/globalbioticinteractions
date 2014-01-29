package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class SearchService {
    private static final Log LOG = LogFactory.getLog(SearchService.class);

    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String COMMON_NAMES = "commonNames";

    @Autowired
    private GraphDatabaseService graphDb;

    @RequestMapping(value = "/findCloseMatchesForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String findCloseMatchesForCommonAndScientificNames(@PathVariable("taxonName") String taxonName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        addCloseMatches(taxonName, buffer, NAME, "taxonNameSuggestions", 0);
        return generateCypherLikeResponse(buffer);
    }

    private String generateCypherLikeResponse(StringBuffer buffer) {
        return "{\"columns\":[\"(taxon.name)\", \"(taxon.commonNames)\", \"(taxon.path)\"],\"data\":[" + buffer.toString() + "]}";
    }

    private int addCloseMatches(String taxonName, StringBuffer buffer, String matchProperty, String indexName, int hitCount) {
        IndexHits<Node> hits = query(taxonName, matchProperty, graphDb.index().forNodes(indexName));
        while (hits.hasNext() && hitCount < 15) {
            if (hitCount > 0) {
                buffer.append(",");
            }
            Node node = hits.next();
            if (node.hasProperty(NAME)) {
                buffer.append("[\"");
                buffer.append((String) node.getProperty(NAME));
                buffer.append("\",\"");
                buffer.append(node.hasProperty(COMMON_NAMES) ? (String) node.getProperty(COMMON_NAMES) : "");
                buffer.append("\",\"");
                buffer.append(node.hasProperty(PATH) ? (String) node.getProperty(PATH) : "");
                buffer.append("\"]");
                hitCount++;
            }

        }
        hits.close();
        return hitCount;
    }

    private IndexHits<Node> query(String taxonName, String name, Index<Node> taxonIndex) {
        StringBuilder builder = new StringBuilder();
        String[] split = StringUtils.split(taxonName, " ");
        for (int i = 0; i < split.length; i++) {
            builder.append("(");
            builder.append(name);
            builder.append(":");
            String part = split[i];
            builder.append(part.toLowerCase());
            builder.append("* OR " );
            builder.append(name);
            builder.append(":");
            builder.append(part.toLowerCase());
            builder.append("~)");
            if (i < (split.length - 1)) {
                builder.append(" AND ");
            }
        }
        String queryString = builder.toString();
        LOG.info("query: [" + queryString + "]");
        return taxonIndex.query(queryString);
    }
}
