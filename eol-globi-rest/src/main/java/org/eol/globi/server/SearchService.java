package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SearchService {

    public static final String NAME = "name";
    public static final String PATH = "path";

    @Autowired
    EmbeddedGraphDatabase graphDb;

    @RequestMapping(value = "/findCloseMatchesForTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findCloseMatchesForTaxon(@PathVariable("taxonName") String taxonName) throws IOException {
        StringBuffer buffer = new StringBuffer();
        addCloseMatches(taxonName, buffer, NAME);
        return "{\"columns\":[\"(taxon.name)\"],\"data\":[" + buffer.toString() + "]}";
    }

    private void addCloseMatches(String taxonName, StringBuffer buffer, String matchProperty) {
        int hitCount = 0;
        IndexHits<Node> query = query(taxonName, matchProperty, graphDb.index().forNodes("taxons"));
        while (query.hasNext() && hitCount < 15) {
            if (hitCount > 0) {
                buffer.append(",");
            }
            Node node = query.next();
            if (node.hasProperty(NAME)) {
                buffer.append("[\"");
                buffer.append((String) node.getProperty(NAME));
                buffer.append("\"]");
                hitCount++;
            }

        }
        query.close();
    }

    private IndexHits<Node> query(String taxonName, String name, Index<Node> taxonIndex) {
        String capitalizedValue = StringUtils.capitalize(taxonName);
        List<Query> list = new ArrayList<Query>();
        addQueriesForProperty(capitalizedValue, name, list);
        BooleanQuery fuzzyAndWildcard = new BooleanQuery();
        for (Query query : list) {
            fuzzyAndWildcard.add(query, BooleanClause.Occur.SHOULD);
        }
        return taxonIndex.query(fuzzyAndWildcard);
    }

    private void addQueriesForProperty(String capitalizedValue, String propertyName, List<Query> list) {
        list.add(new FuzzyQuery(new Term(propertyName, capitalizedValue)));
        list.add(new WildcardQuery(new Term(propertyName, capitalizedValue + "*")));

    }
}
