package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import java.util.ArrayList;
import java.util.List;

public class NodeUtil {
    public static String getPropertyStringValueOrNull(Node node, String propertyName) {
        return node.hasProperty(propertyName) ? (String) node.getProperty(propertyName) : null;
    }

    public static String truncateTaxonName(String taxonName) {
        String truncatedName = null;
        String[] nameParts = StringUtils.split(taxonName);
        if (nameParts.length > 2) {
            truncatedName = nameParts[0].trim() + " " + nameParts[1].trim();
        } else if (nameParts.length > 1) {
            truncatedName = nameParts[0];
        }
        return truncatedName;
    }

    public static IndexHits<Node> query(String taxonName, String name, Index<Node> taxonIndex) {
        String capitalizedValue = StringUtils.capitalize(taxonName);
        List<Query> list = new ArrayList<Query>();
        addQueriesForProperty(capitalizedValue, name, list);
        BooleanQuery fuzzyAndWildcard = new BooleanQuery();
        for (Query query : list) {
            fuzzyAndWildcard.add(query, BooleanClause.Occur.SHOULD);
        }
        return taxonIndex.query(fuzzyAndWildcard);
    }

    private static void addQueriesForProperty(String capitalizedValue, String propertyName, List<Query> list) {
        list.add(new FuzzyQuery(new Term(propertyName, capitalizedValue)));
        list.add(new WildcardQuery(new Term(propertyName, capitalizedValue + "*")));
    }
}
