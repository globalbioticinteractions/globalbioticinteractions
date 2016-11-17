package org.eol.globi.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum QueryType {
    SINGLE_TAXON_DISTINCT, SINGLE_TAXON_ALL, MULTI_TAXON_DISTINCT, MULTI_TAXON_DISTINCT_BY_NAME_ONLY, MULTI_TAXON_ALL;


    public static QueryType forParams(Map parameterMap) {
        QueryType queryType = MULTI_TAXON_DISTINCT;

        if (shouldIncludeObservations(parameterMap)) {
            queryType = MULTI_TAXON_ALL;
        } else if (isTaxonQueryOnly(parameterMap)) {
            queryType = MULTI_TAXON_DISTINCT_BY_NAME_ONLY;
        }
        return queryType;
    }

    public static QueryType forParamsSingle(Map parameterMap) {
        return shouldIncludeObservations(parameterMap)
                ? SINGLE_TAXON_ALL
                : SINGLE_TAXON_DISTINCT;
    }


    private static boolean shouldIncludeObservations(Map parameterMap) {
        List<String> includeObservations = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.INCLUDE_OBSERVATIONS);
        return includeObservations.size() > 0
                && ("t".equalsIgnoreCase(includeObservations.get(0)) || "true".equalsIgnoreCase(includeObservations.get(0)));
    }

    private static boolean isTaxonQueryOnly(Map parameterMap) {
        List<String> accordingTo = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        List<String> bbox = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.BBOX);
        return accordingTo.isEmpty() && bbox.isEmpty();
    }

    public static boolean usesSpecimenData(QueryType queryType) {
        List<QueryType> queryTypes = Arrays.asList(
                MULTI_TAXON_ALL,
                MULTI_TAXON_DISTINCT
        );
        return queryTypes.contains(queryType);
    }

    public static boolean isDistinct(QueryType queryType) {
        return MULTI_TAXON_DISTINCT.equals(queryType);
    }
}
