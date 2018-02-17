package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.server.util.ResultObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eol.globi.server.CypherQueryBuilder.collectParamValues;
import static org.eol.globi.server.util.ResultField.*;

public class CypherReturnClauseBuilder {

    private static final ResultField[] RETURN_FIELDS_MULTI_TAXON_DEFAULT = new ResultField[]{
            SOURCE_TAXON_EXTERNAL_ID, SOURCE_TAXON_NAME, SOURCE_TAXON_PATH, SOURCE_SPECIMEN_LIFE_STAGE, SOURCE_SPECIMEN_BASIS_OF_RECORD,
            INTERACTION_TYPE,
            TARGET_TAXON_EXTERNAL_ID, TARGET_TAXON_NAME, TARGET_TAXON_PATH, TARGET_SPECIMEN_LIFE_STAGE, TARGET_SPECIMEN_BASIS_OF_RECORD,
            LATITUDE, LONGITUDE, STUDY_TITLE};

    private static final ResultField[] RETURN_FIELDS_SINGLE_TAXON_DEFAULT = new ResultField[]{SOURCE_TAXON_NAME, INTERACTION_TYPE, TARGET_TAXON_NAME,
            LATITUDE, LONGITUDE, ALTITUDE, STUDY_TITLE, COLLECTION_TIME_IN_UNIX_EPOCH,
            SOURCE_SPECIMEN_ID,
            TARGET_SPECIMEN_ID,
            SOURCE_SPECIMEN_LIFE_STAGE,
            TARGET_SPECIMEN_LIFE_STAGE,
            SOURCE_SPECIMEN_BASIS_OF_RECORD,
            TARGET_SPECIMEN_BASIS_OF_RECORD,
            SOURCE_SPECIMEN_BODY_PART,
            TARGET_SPECIMEN_BODY_PART,
            SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE,
            TARGET_SPECIMEN_PHYSIOLOGICAL_STATE,
            TARGET_SPECIMEN_TOTAL_COUNT,
            TARGET_SPECIMEN_TOTAL_COUNT_PERCENT,
            TARGET_SPECIMEN_TOTAL_VOLUME_ML,
            TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT,
            TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE,
            TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT,
            FOOTPRINT_WKT,
            LOCALITY
    };

    private static Map<ResultField, String> appendStudyFields(Map<ResultField, String> selectors) {
        return new HashMap<ResultField, String>(selectors) {
            {
                put(STUDY_TITLE, ResultObject.STUDY.getLabel() + ".title");
                put(STUDY_URL, ResultObject.STUDY.getLabel() + ".externalId?");
                put(STUDY_DOI, ResultObject.STUDY.getLabel() + ".doi?");
                put(STUDY_CITATION, ResultObject.STUDY.getLabel() + ".citation?");
                put(STUDY_SOURCE_CITATION, ResultObject.STUDY.getLabel() + ".source?");
            }
        };
    }

    private static Map<ResultField, String> appendSpecimenFields(Map<ResultField, String> selectors) {
        return new HashMap<ResultField, String>(selectors) {
            {
                put(SOURCE_SPECIMEN_ID, "ID(" + ResultObject.SOURCE_SPECIMEN.getLabel() + ")");
                put(TARGET_SPECIMEN_ID, "ID(" + ResultObject.TARGET_SPECIMEN.getLabel() + ")");
                put(TARGET_SPECIMEN_TOTAL_COUNT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.TOTAL_COUNT + "?");
                put(TARGET_SPECIMEN_TOTAL_COUNT_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.TOTAL_COUNT_PERCENT + "?");
                put(TARGET_SPECIMEN_TOTAL_VOLUME_ML, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.TOTAL_VOLUME_IN_ML + "?");
                put(TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.TOTAL_VOLUME_PERCENT + "?");
                put(TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.FREQUENCY_OF_OCCURRENCE + "?");
                put(TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.FREQUENCY_OF_OCCURRENCE_PERCENT + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_LABEL + "?");
                put(SOURCE_SPECIMEN_BASIS_OF_RECORD, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.BASIS_OF_RECORD_LABEL + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_LABEL + "?");
                put(TARGET_SPECIMEN_BASIS_OF_RECORD, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.BASIS_OF_RECORD_LABEL + "?");

            }
        };
    }

    static void appendReturnClauseMap(StringBuilder query, QueryType queryType, Map parameterMap) {
        List<String> requestedReturnFields = CypherQueryBuilder.collectRequestedFields(parameterMap);

        if (QueryType.isDistinct(queryType) && QueryType.usesSpecimenData(queryType)) {
            query.append(" WITH distinct ")
                    .append(ResultObject.TARGET_TAXON.getLabel())
                    .append(", ")
                    .append(ResultObject.INTERACTION.getLabel())
                    .append(".label as ")
                    .append(ResultObject.INTERACTION_TYPE.getLabel())
                    .append(", ")
                    .append(ResultObject.SOURCE_TAXON.getLabel());

            appendAggregateCountersIfNeeded(query, requestedReturnFields);
        }


        query.append(" ");

        appendTaxonIdPrefixClause(query, queryType, parameterMap, requestedReturnFields);
        appendReturnClause(query, queryType, requestedReturnFields);

        List<String> counters = QueryType.aggregateCountersIn(requestedReturnFields);
        // see #330 - avoid row scan in combination with order by
        if (!counters.isEmpty()
                && (collectParamValues(parameterMap, ParamName.SOURCE_TAXON).isEmpty()
                || collectParamValues(parameterMap, ParamName.TARGET_TAXON).isEmpty())
                ) {
            //query.append(" ORDER BY ");
            //query.append(StringUtils.join(counters.stream().map(c -> c + " DESC").collect(Collectors.toList()), ", "));

        }
    }

    private static void appendAggregateCountersIfNeeded(StringBuilder query, List<String> requestedFields) {
        if (requestedFields.contains(ResultField.NUMBER_OF_INTERACTIONS.getLabel())) {
            query.append(", count(").append(ResultObject.INTERACTION.getLabel())
                    .append(") as ")
                    .append(ResultObject.INTERACTION_COUNT.getLabel());
        }

        if (requestedFields.contains(ResultField.NUMBER_OF_STUDIES.getLabel())) {
            query.append(", count(distinct(id(").append(ResultObject.STUDY.getLabel())
                    .append("))) as ")
                    .append(ResultObject.STUDY_COUNT.getLabel());
        }

        if (requestedFields.contains(ResultField.NUMBER_OF_SOURCES.getLabel())) {
            query.append(", count(distinct(").append(ResultObject.STUDY.getLabel())
                    .append(".source?)) as ")
                    .append(ResultObject.STUDY_SOURCE_COUNT.getLabel());
        }
    }

    private static void appendTaxonIdPrefixClause(StringBuilder query, QueryType queryType, Map parameterMap, List<String> requestedReturnFields) {
        List<String> prefixes = collectParamValues(parameterMap, ParamName.TAXON_ID_PREFIX);
        if (!prefixes.isEmpty()) {
            String sourceLabel = ResultObject.SOURCE_TAXON.getLabel();

            String interactionLabel = QueryType.isDistinct(queryType)
                    ? ResultObject.INTERACTION_TYPE.getLabel()
                    : ResultObject.INTERACTION.getLabel();

            String targetLabel = ResultObject.TARGET_TAXON.getLabel();

            query.append("WITH ");
            List<String> inParams;
            if (QueryType.usesSpecimenData(queryType) && !QueryType.isDistinct(queryType)) {
                inParams = Arrays.asList(sourceLabel,
                        ResultObject.SOURCE_SPECIMEN.getLabel(),
                        ResultObject.INTERACTION.getLabel(),
                        targetLabel,
                        ResultObject.TARGET_SPECIMEN.getLabel(),
                        ResultObject.LOCATION.getLabel(),
                        ResultObject.STUDY.getLabel());
            } else {
                List<String> defaultInParams = Arrays.asList(sourceLabel, interactionLabel, targetLabel);
                inParams = appendCountersIfNeeded(requestedReturnFields, defaultInParams);
            }
            query.append(StringUtils.join(inParams, ", ")).append(" ");
            query.append(taxonIdPrefixWithMatch(sourceLabel, targetLabel));

            query.append("WITH ");
            List<String> outParams;
            if (QueryType.usesSpecimenData(queryType) && !QueryType.isDistinct(queryType)) {
                outParams = Arrays.asList(sameAs(sourceLabel),
                        ResultObject.SOURCE_SPECIMEN.getLabel(),
                        ResultObject.INTERACTION.getLabel(),
                        sameAs(targetLabel),
                        ResultObject.TARGET_SPECIMEN.getLabel(),
                        ResultObject.LOCATION.getLabel(),
                        ResultObject.STUDY.getLabel());
            } else {
                List<String> defaultOutParams = Arrays.asList(sameAs(sourceLabel), interactionLabel, sameAs(targetLabel));
                outParams = appendCountersIfNeeded(requestedReturnFields, defaultOutParams);
            }

            query.append(StringUtils.join(outParams, ", ")).append(" ");
        }
    }

    private static List<String> appendCountersIfNeeded(List<String> requestedReturnFields, List<String> defaultInParams) {
        List<String> inParams;
        inParams = new ArrayList<>(defaultInParams);
        if (requestedReturnFields.contains(ResultField.NUMBER_OF_INTERACTIONS.getLabel())) {
            inParams.add(ResultObject.INTERACTION_COUNT.getLabel());
        }

        if (requestedReturnFields.contains(ResultField.NUMBER_OF_STUDIES.getLabel())) {
            inParams.add(ResultObject.STUDY_COUNT.getLabel());
        }

        if (requestedReturnFields.contains(ResultField.NUMBER_OF_SOURCES.getLabel())) {
            inParams.add(ResultObject.STUDY_SOURCE_COUNT.getLabel());
        }
        return inParams;
    }

    private static String sameAs(String sourceLabel) {
        return sameAsLabel(sourceLabel) + " as " + sourceLabel;
    }

    private static String sameAsLabel(String label) {
        return label + "SameAs";
    }

    private static void appendReturnClause(StringBuilder query, QueryType queryType, List<String> requestedReturnFields) {
        switch (queryType) {
            case SINGLE_TAXON_DISTINCT:
                Map<ResultField, String> selectors1 = new HashMap<ResultField, String>() {
                    {
                        put(SOURCE_TAXON_NAME, ResultObject.SOURCE_TAXON.getLabel() + ".name");
                        put(INTERACTION_TYPE, ResultObject.INTERACTION.getLabel() + ".label");
                        put(TARGET_TAXON_NAME, "collect(distinct(" + ResultObject.TARGET_TAXON.getLabel() + ".name))");
                    }
                };
                List<ResultField> returnFields1 = Arrays.asList(SOURCE_TAXON_NAME, INTERACTION_TYPE, TARGET_TAXON_NAME);
                appendReturnClauseDistinctz(query, returnFields1, selectors1);
                break;
            case SINGLE_TAXON_ALL:
                Map<ResultField, String> selectors = appendSpecimenFields(
                        appendStudyFields(new HashMap<ResultField, String>(defaultSelectors()) {
                            {
                                put(INTERACTION_TYPE, ResultObject.INTERACTION.getLabel() + ".label");
                                put(COLLECTION_TIME_IN_UNIX_EPOCH, ResultObject.COLLECTED_REL.getLabel() + ".dateInUnixEpoch?");

                            }
                        }));
                appendReturnClause(query, actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_SINGLE_TAXON_DEFAULT), selectors.keySet()), selectors);
                break;
            case MULTI_TAXON_ALL:
                selectors = appendSpecimenFields(
                        appendStudyFields(new HashMap<ResultField, String>(defaultSelectors()) {
                            {
                                put(INTERACTION_TYPE, ResultObject.INTERACTION.getLabel() + ".label");
                                put(NUMBER_OF_INTERACTIONS, "1");
                                put(NUMBER_OF_STUDIES, "1");
                                put(NUMBER_OF_SOURCES, "1");
                            }
                        }));
                appendReturnClauseDistinctz(query, actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_MULTI_TAXON_DEFAULT), selectors.keySet()), selectors);
                break;
            case MULTI_TAXON_DISTINCT:
                Map<ResultField, String> actualSelectors = defaultSelectors(ResultObject.SOURCE_TAXON_DISTINCT.getLabel(), ResultObject.TARGET_TAXON_DISTINCT.getLabel());
                selectors = new HashMap<ResultField, String>(actualSelectors) {
                    {
                        put(SOURCE_TAXON_EXTERNAL_ID, ResultObject.SOURCE_TAXON_DISTINCT.getLabel() + ".externalId?");
                        put(SOURCE_TAXON_NAME, ResultObject.SOURCE_TAXON_DISTINCT.getLabel() + ".name");
                        put(SOURCE_TAXON_PATH, ResultObject.SOURCE_TAXON_DISTINCT.getLabel() + ".path?");
                        put(SOURCE_SPECIMEN_LIFE_STAGE, "NULL");
                        put(SOURCE_SPECIMEN_BASIS_OF_RECORD, "NULL");
                        put(INTERACTION_TYPE, ResultObject.INTERACTION_TYPE.getLabel());
                        put(NUMBER_OF_INTERACTIONS, ResultObject.INTERACTION_COUNT.getLabel());
                        put(NUMBER_OF_STUDIES, ResultObject.STUDY_COUNT.getLabel());
                        put(NUMBER_OF_SOURCES, ResultObject.STUDY_SOURCE_COUNT.getLabel());
                        put(TARGET_TAXON_EXTERNAL_ID, ResultObject.TARGET_TAXON_DISTINCT.getLabel() + ".externalId?");
                        put(TARGET_TAXON_NAME, ResultObject.TARGET_TAXON_DISTINCT.getLabel() + ".name");
                        put(TARGET_TAXON_PATH, ResultObject.TARGET_TAXON_DISTINCT.getLabel() + ".path?");
                        put(TARGET_SPECIMEN_LIFE_STAGE, "NULL");
                        put(TARGET_SPECIMEN_BASIS_OF_RECORD, "NULL");
                        put(LATITUDE, "NULL");
                        put(LONGITUDE, "NULL");
                        put(STUDY_TITLE, "NULL");
                        put(STUDY_URL, "NULL");
                        put(STUDY_DOI, "NULL");
                        put(STUDY_CITATION, "NULL");
                        put(STUDY_SOURCE_CITATION, "NULL");
                    }
                };
                List<ResultField> returnFields = actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_MULTI_TAXON_DEFAULT), selectors.keySet());
                appendReturnClauseDistinctz(query, returnFields, selectors);
                break;
            case MULTI_TAXON_DISTINCT_BY_NAME_ONLY:
                selectors = new HashMap<ResultField, String>(defaultSelectors()) {
                    {
                        put(INTERACTION_TYPE, ResultObject.INTERACTION.getLabel() + ".label?");
                        put(NUMBER_OF_INTERACTIONS, ResultObject.INTERACTION.getLabel() + ".count");
                        put(SOURCE_SPECIMEN_LIFE_STAGE, "NULL");
                        put(SOURCE_SPECIMEN_BASIS_OF_RECORD, "NULL");
                        put(TARGET_SPECIMEN_LIFE_STAGE, "NULL");
                        put(TARGET_SPECIMEN_BASIS_OF_RECORD, "NULL");
                        put(LATITUDE, "NULL");
                        put(LONGITUDE, "NULL");
                        put(STUDY_TITLE, "NULL");
                        put(STUDY_URL, "NULL");
                        put(STUDY_DOI, "NULL");
                        put(STUDY_CITATION, "NULL");
                        put(STUDY_SOURCE_CITATION, "NULL");
                    }
                };
                appendReturnClauseDistinctz(query, actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_MULTI_TAXON_DEFAULT), selectors.keySet()), selectors);
                break;

            default:
                throw new IllegalArgumentException("invalid option [" + queryType + "]");
        }
    }

    static List<ResultField> actualReturnFields(List<String> requestedReturnFields, List<ResultField> defaultReturnFields, Collection<ResultField> availableReturnFields) {
        List<ResultField> returnFields = new ArrayList<>();
        for (String requestedReturnField : requestedReturnFields) {
            for (ResultField resultField : values()) {
                if (resultField.getLabel().equals(requestedReturnField)) {
                    if (availableReturnFields.contains(resultField)) {
                        returnFields.add(resultField);
                    }
                    break;
                }
            }

        }
        return returnFields.size() == 0 ? Collections.unmodifiableList(defaultReturnFields) : Collections.unmodifiableList(returnFields);
    }

    private static void appendReturnClause(StringBuilder query, List<ResultField> returnFields, Map<ResultField, String> selectors) {
        query.append(" RETURN ");
        appendReturnFields(query, returnFields, selectors);
    }

    private static Map<ResultField, String> defaultSelectors() {
        return defaultSelectors("sourceTaxon", "targetTaxon");
    }

    private static Map<ResultField, String> defaultSelectors(final String sourceTaxonPrefix, final String targetTaxonPrefix) {
        return new HashMap<ResultField, String>() {
            {
                addSourceTaxonFields(sourceTaxonPrefix);
                addTargetTaxonFields(targetTaxonPrefix);
                put(LATITUDE, ResultObject.LOCATION.getLabel() + "." + LocationConstant.LATITUDE + "?");
                put(LONGITUDE, ResultObject.LOCATION.getLabel() + "." + LocationConstant.LONGITUDE + "?");
                put(ALTITUDE, ResultObject.LOCATION.getLabel() + "." + LocationConstant.ALTITUDE + "?");
                put(FOOTPRINT_WKT, ResultObject.LOCATION.getLabel() + "." + LocationConstant.FOOTPRINT_WKT + "?");
                put(LOCALITY, ResultObject.LOCATION.getLabel() + "." + LocationConstant.LOCALITY + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_LABEL + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE_ID, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_ID + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_LABEL + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE_ID, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.LIFE_STAGE_ID + "?");
                put(SOURCE_SPECIMEN_BODY_PART, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.BODY_PART_LABEL + "?");
                put(SOURCE_SPECIMEN_BODY_PART_ID, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.BODY_PART_ID + "?");
                put(TARGET_SPECIMEN_BODY_PART, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.BODY_PART_LABEL + "?");
                put(TARGET_SPECIMEN_BODY_PART_ID, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.BODY_PART_ID + "?");
                put(SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + "?");
                put(SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE_ID, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + "?");
                put(TARGET_SPECIMEN_PHYSIOLOGICAL_STATE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.PHYSIOLOGICAL_STATE_LABEL + "?");
                put(TARGET_SPECIMEN_PHYSIOLOGICAL_STATE_ID, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.PHYSIOLOGICAL_STATE_ID + "?");
                put(SOURCE_SPECIMEN_BASIS_OF_RECORD, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + SpecimenConstant.BASIS_OF_RECORD_LABEL + "?");
                put(TARGET_SPECIMEN_BASIS_OF_RECORD, ResultObject.TARGET_SPECIMEN.getLabel() + "." + SpecimenConstant.BASIS_OF_RECORD_LABEL + "?");
            }

            private void addTargetTaxonFields(String prefix) {
                put(TARGET_TAXON_NAME, prefix + ".name");
                put(TARGET_TAXON_COMMON_NAMES, prefix + ".commonNames?");
                put(TARGET_TAXON_EXTERNAL_ID, prefix + ".externalId?");
                put(TARGET_TAXON_PATH, prefix + ".path?");
                put(TARGET_TAXON_PATH_RANKS, prefix + ".pathNames?");
                put(TARGET_TAXON_PATH_IDS, prefix + ".pathIds?");
            }

            private void addSourceTaxonFields(String prefix) {
                put(SOURCE_TAXON_NAME, prefix + ".name");
                put(SOURCE_TAXON_COMMON_NAMES, prefix + ".commonNames?");
                put(SOURCE_TAXON_EXTERNAL_ID, prefix + ".externalId?");
                put(SOURCE_TAXON_PATH, prefix + ".path?");
                put(SOURCE_TAXON_PATH_RANKS, prefix + ".pathNames?");
                put(SOURCE_TAXON_PATH_IDS, prefix + ".pathIds?");
            }
        };
    }

    static void appendReturnClauseDistinctz(StringBuilder query, List<ResultField> returnFields, Map<ResultField, String> selectors) {
        query.append("RETURN ");
        appendReturnFields(query, returnFields, selectors);
    }

    private static void appendReturnFields(StringBuilder query, List<ResultField> fields, Map<ResultField, String> selectors) {
        List<String> returnFields = new ArrayList<String>();
        for (ResultField field : fields) {
            returnFields.add(selectors.get(field) + " as " + field);
        }
        query.append(StringUtils.join(returnFields, ","));
    }

    private static String taxonIdPrefixWithMatch(String sourceTaxonLabel, String targetTaxonLabel) {
        return "MATCH " + sourceTaxonLabel + "-[:SAME_AS*0..1]->" + sameAsLabel(sourceTaxonLabel) +
                ", " + targetTaxonLabel + "-[:SAME_AS*0..1]->" + sameAsLabel(targetTaxonLabel) + " " +
                "WHERE " + sameAsLabel(sourceTaxonLabel) + ".externalId =~ {source_taxon_prefix} " +
                "AND " + sameAsLabel(targetTaxonLabel) + ".externalId =~ {target_taxon_prefix} ";
    }

}
