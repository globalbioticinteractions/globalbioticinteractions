package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.server.util.ResultObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static final List<CypherQueryBuilder.QueryType> QUERY_TYPES_DISTINCT = Arrays.asList(
             CypherQueryBuilder.QueryType.MULTI_TAXON_DISTINCT
    );

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
                put(TARGET_SPECIMEN_TOTAL_COUNT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.TOTAL_COUNT + "?");
                put(TARGET_SPECIMEN_TOTAL_COUNT_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.TOTAL_COUNT_PERCENT + "?");
                put(TARGET_SPECIMEN_TOTAL_VOLUME_ML, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.TOTAL_VOLUME_IN_ML + "?");
                put(TARGET_SPECIMEN_TOTAL_VOLUME_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.TOTAL_VOLUME_PERCENT + "?");
                put(TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.FREQUENCY_OF_OCCURRENCE + "?");
                put(TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE_PERCENT, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.FREQUENCY_OF_OCCURRENCE_PERCENT + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.LIFE_STAGE_LABEL + "?");
                put(SOURCE_SPECIMEN_BASIS_OF_RECORD, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.BASIS_OF_RECORD_LABEL + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.LIFE_STAGE_LABEL + "?");
                put(TARGET_SPECIMEN_BASIS_OF_RECORD, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.BASIS_OF_RECORD_LABEL + "?");

            }
        };
    }

    static void appendReturnClauseMap(StringBuilder query, CypherQueryBuilder.QueryType queryType, Map parameterMap) {
        if (isDistinct(queryType)) {
            query.append("WITH distinct " + ResultObject.TARGET_TAXON.getLabel() + " as " + ResultObject.TARGET_TAXON_DISTINCT.getLabel() + ", "
                    + ResultObject.INTERACTION.getLabel() + ".label as " + ResultObject.INTERACTION_TYPE.getLabel() + ", " +
                    ResultObject.SOURCE_TAXON.getLabel() + " as " + ResultObject.SOURCE_TAXON_DISTINCT.getLabel() + " ");
        }
        appendTaxonIdPrefixClause(query, queryType, parameterMap);
        appendReturnClause(query, queryType, CypherQueryBuilder.collectRequestedFields(parameterMap));
    }

    private static void appendTaxonIdPrefixClause(StringBuilder query, CypherQueryBuilder.QueryType queryType, Map parameterMap) {
        List<String> prefixes = CypherQueryBuilder.collectParamValues(parameterMap, ParamName.TAXON_ID_PREFIX);
        if (!prefixes.isEmpty()) {
            String sourcePrefix = isDistinct(queryType) ? "s" : "source";
            String targetPrefix = isDistinct(queryType) ? "t" : "target";

            String sourceLabel = sourcePrefix + "Taxon";
            String targetLabel = targetPrefix + "Taxon";

            query.append("WITH ");
            List<String> inParams;
            if (isDistinct(queryType)) {
                inParams = Arrays.asList(sourceLabel, ResultObject.INTERACTION_TYPE.getLabel(), targetLabel);
            } else {
                inParams = Arrays.asList(sourceLabel,
                        ResultObject.SOURCE_SPECIMEN.getLabel(),
                        ResultObject.INTERACTION.getLabel(),
                        targetLabel,
                        ResultObject.TARGET_SPECIMEN.getLabel(),
                        ResultObject.LOCATION.getLabel(),
                        ResultObject.STUDY.getLabel());
            }
            query.append(StringUtils.join(inParams, ", ")).append(" ");
            query.append(taxonIdPrefixWithMatch(sourceLabel, targetLabel));

            query.append("WITH ");
            List<String> outParams;
            if (isDistinct(queryType)) {
                outParams = Arrays.asList(sameAs(sourceLabel), ResultObject.INTERACTION_TYPE.getLabel(), sameAs(targetLabel));
            } else {
                outParams = Arrays.asList(sameAs(sourceLabel),
                        ResultObject.SOURCE_SPECIMEN.getLabel(),
                        ResultObject.INTERACTION.getLabel(),
                        sameAs(targetLabel),
                        ResultObject.TARGET_SPECIMEN.getLabel(),
                        ResultObject.LOCATION.getLabel(),
                        ResultObject.STUDY.getLabel());
            }

            query.append(StringUtils.join(outParams, ", ")).append(" ");
        }
    }

    private static String sameAs(String sourceLabel) {
        return sameAsLabel(sourceLabel) + " as " + sourceLabel;
    }

    private static String sameAsLabel(String label) {
        return label + "SameAs";
    }

    private static boolean isDistinct(CypherQueryBuilder.QueryType queryType) {
        return QUERY_TYPES_DISTINCT.contains(queryType);
    }

    private static void appendReturnClause(StringBuilder query, CypherQueryBuilder.QueryType queryType, List<String> requestedReturnFields) {
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
        List<ResultField> returnFields = new ArrayList<ResultField>();
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
                put(LATITUDE, ResultObject.LOCATION.getLabel() + "." + LocationNode.LATITUDE + "?");
                put(LONGITUDE, ResultObject.LOCATION.getLabel() + "." + LocationNode.LONGITUDE + "?");
                put(ALTITUDE, ResultObject.LOCATION.getLabel() + "." + LocationNode.ALTITUDE + "?");
                put(FOOTPRINT_WKT, ResultObject.LOCATION.getLabel() + "." + LocationNode.FOOTPRINT_WKT + "?");
                put(LOCALITY, ResultObject.LOCATION.getLabel() + "." + LocationNode.LOCALITY + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.LIFE_STAGE_LABEL + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.LIFE_STAGE_LABEL + "?");
                put(SOURCE_SPECIMEN_BODY_PART, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.BODY_PART_LABEL + "?");
                put(TARGET_SPECIMEN_BODY_PART, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.BODY_PART_LABEL + "?");
                put(SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "?");
                put(TARGET_SPECIMEN_PHYSIOLOGICAL_STATE, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "?");
                put(SOURCE_SPECIMEN_BASIS_OF_RECORD, ResultObject.SOURCE_SPECIMEN.getLabel() + "." + Specimen.BASIS_OF_RECORD_LABEL + "?");
                put(TARGET_SPECIMEN_BASIS_OF_RECORD, ResultObject.TARGET_SPECIMEN.getLabel() + "." + Specimen.BASIS_OF_RECORD_LABEL + "?");
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
