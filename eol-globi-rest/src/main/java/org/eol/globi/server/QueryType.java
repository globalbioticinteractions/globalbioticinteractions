package org.eol.globi.server;

import org.eol.globi.server.util.ResultField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<String> fields = CypherQueryBuilder.collectRequestedFields(parameterMap);
        return accordingTo.isEmpty() && bbox.isEmpty() && !containsAggregateCountersExcludingInteractionCount(fields);
    }

    public static boolean containsAggregateCountersExcludingInteractionCount(List<String> fields) {
        return !aggregateCountersExcludingInteractionCountIn(fields).isEmpty();
    }

    public static List<String> aggregateCountersExcludingInteractionCountIn(List<String> fields) {
        List<String> aggregateCounters = Arrays.asList(ResultField.NUMBER_OF_SOURCES.getLabel(), ResultField.NUMBER_OF_STUDIES.getLabel());
        return aggregateCountersIn(fields, aggregateCounters);
    }

    public static List<String> aggregateCountersIn(List<String> fields) {
        List<String> counterLabels = Arrays.asList(ResultField.NUMBER_OF_SOURCES.getLabel(), ResultField.NUMBER_OF_STUDIES.getLabel(), ResultField.NUMBER_OF_INTERACTIONS.getLabel());
        return aggregateCountersIn(fields, counterLabels);
    }

    public static List<String> aggregateCountersIn(List<String> fields, List<String> counterLabels) {
        return counterLabels.stream().filter(fields::contains).collect(Collectors.toList());
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
