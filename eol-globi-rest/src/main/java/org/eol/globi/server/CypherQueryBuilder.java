package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.server.util.InteractionTypeExternal;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.eol.globi.server.util.ResultField.ALTITUDE;
import static org.eol.globi.server.util.ResultField.COLLECTION_TIME_IN_UNIX_EPOCH;
import static org.eol.globi.server.util.ResultField.INTERACTION_TYPE;
import static org.eol.globi.server.util.ResultField.LATITUDE;
import static org.eol.globi.server.util.ResultField.LONGITUDE;
import static org.eol.globi.server.util.ResultField.SOURCE_SPECIMEN_BODY_PART;
import static org.eol.globi.server.util.ResultField.SOURCE_SPECIMEN_ID;
import static org.eol.globi.server.util.ResultField.SOURCE_SPECIMEN_LIFE_STAGE;
import static org.eol.globi.server.util.ResultField.SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_COMMON_NAMES;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_EXTERNAL_ID;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_NAME;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_PATH;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_PATH_IDS;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_PATH_RANKS;
import static org.eol.globi.server.util.ResultField.STUDY_TITLE;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_BODY_PART;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_ID;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_LIFE_STAGE;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_PHYSIOLOGICAL_STATE;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_TOTAL_COUNT;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE;
import static org.eol.globi.server.util.ResultField.TARGET_SPECIMEN_TOTAL_VOLUME_ML;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_COMMON_NAMES;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_EXTERNAL_ID;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_PATH;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_PATH_IDS;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_PATH_RANKS;
import static org.eol.globi.server.util.ResultField.TAXON_COMMON_NAMES;
import static org.eol.globi.server.util.ResultField.TAXON_EXTERNAL_ID;
import static org.eol.globi.server.util.ResultField.TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TAXON_PATH;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_IDS;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_RANKS;

public class CypherQueryBuilder {
    private static final Log LOG = LogFactory.getLog(CypherQueryBuilder.class);

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";

    public static final String INTERACTION_PARASITE_OF = "parasiteOf";
    public static final String INTERACTION_HAS_PARASITE = "hasParasite";

    public static final String INTERACTION_POLLINATES = "pollinates";
    public static final String INTERACTION_POLLINATED_BY = "pollinatedBy";

    public static final String INTERACTION_PATHOGEN_OF = "pathogenOf";
    public static final String INTERACTION_HAS_PATHOGEN = "hasPathogen";

    public static final String INTERACTION_VECTOR_OF = "vectorOf";
    public static final String INTERACTION_HAS_VECTOR = "hasVector";

    public static final String INTERACTION_HOST_OF = "hostOf";
    public static final String INTERACTION_HAS_HOST = "hasHost";

    public static final String INTERACTION_SYMBIONT_OF = "symbiontOf";
    public static final String INTERACTION_INTERACTS_WITH = "interactsWith";

    private static final String SOURCE_TAXON_HTTP_PARAM_NAME = "sourceTaxon";
    private static final String TARGET_TAXON_HTTP_PARAM_NAME = "targetTaxon";

    public static final String TAXON_HTTP_PARAM_NAME = "taxon";

    private static final Map<String, String> DIRECTIONAL_INTERACTION_TYPE_MAP = new TreeMap<String, String>() {
        {
            String preysOn = InteractType.ATE + "|" + InteractType.PREYS_UPON;
            put(INTERACTION_PREYS_ON, preysOn);
            put(INTERACTION_PREYED_UPON_BY, InteractType.EATEN_BY + "|" + InteractType.PREYED_UPON_BY);
            put(INTERACTION_PARASITE_OF, InteractType.PARASITE_OF.toString());
            put(INTERACTION_HAS_PARASITE, InteractType.HAS_PARASITE.toString());
            put(INTERACTION_POLLINATES, InteractType.POLLINATES.toString());
            put(INTERACTION_POLLINATED_BY, InteractType.POLLINATED_BY.toString());
            put(INTERACTION_PATHOGEN_OF, InteractType.PATHOGEN_OF.toString());
            put(INTERACTION_HAS_PATHOGEN, InteractType.HAS_PATHOGEN.toString());
            put(INTERACTION_HAS_VECTOR, InteractType.HAS_VECTOR.toString());
            put(INTERACTION_VECTOR_OF, InteractType.VECTOR_OF.toString());
            put(INTERACTION_HOST_OF, StringUtils.join(new String[]{InteractType.HOST_OF.toString(), InteractType.HAS_PARASITE.toString(), InteractType.HAS_PATHOGEN.toString()}, "|"));
            put(INTERACTION_HAS_HOST, StringUtils.join(new String[]{InteractType.HAS_HOST.toString(),InteractType.PARASITE_OF.toString(), InteractType.PATHOGEN_OF.toString()}, "|"));
            put(INTERACTION_SYMBIONT_OF, StringUtils.join(InteractType.values(), "|"));
            put(INTERACTION_INTERACTS_WITH, StringUtils.join(InteractType.values(), "|"));
        }
    };

    public static final Map<String, InteractionTypeExternal> INTERACTION_TYPE_INTERNAL_EXTERNAL_MAP = new TreeMap<String, InteractionTypeExternal>() {
        {
            put(InteractType.ATE.toString(), InteractionTypeExternal.PREYS_ON);
            put(InteractType.PREYS_UPON.toString(), InteractionTypeExternal.PREYS_ON);
            put(InteractType.EATEN_BY.toString(), InteractionTypeExternal.PREYED_UPON_BY);
            put(InteractType.PREYED_UPON_BY.toString(), InteractionTypeExternal.PREYED_UPON_BY);
            put(InteractType.PARASITE_OF.toString(), InteractionTypeExternal.PARASITE_OF);
            put(InteractType.HAS_PARASITE.toString(), InteractionTypeExternal.HAS_PARASITE);
            put(InteractType.POLLINATES.toString(), InteractionTypeExternal.POLLINATES);
            put(InteractType.POLLINATED_BY.toString(), InteractionTypeExternal.POLLINATED_BY);
            put(InteractType.PATHOGEN_OF.toString(), InteractionTypeExternal.PATHOGEN_OF);
            put(InteractType.HAS_PATHOGEN.toString(), InteractionTypeExternal.HAS_PATHOGEN);
            put(InteractType.VECTOR_OF.toString(), InteractionTypeExternal.VECTOR_OF);
            put(InteractType.HAS_VECTOR.toString(), InteractionTypeExternal.HAS_VECTOR);
            put(InteractType.SYMBIONT_OF.toString(), InteractionTypeExternal.SYMBIONT_OF);
            put(InteractType.INTERACTS_WITH.toString(), InteractionTypeExternal.INTERACTS_WITH);
            put(InteractType.HAS_HOST.toString(), InteractionTypeExternal.HAS_HOST);
            put(InteractType.HOST_OF.toString(), InteractionTypeExternal.HOST_OF);
        }
    };

    private static final Map<String, String> TRANSLATION_MAP = new TreeMap<String, String>() {
        {
            String preysOn = InteractType.ATE + "|" + InteractType.PREYS_UPON;
            put(INTERACTION_PREYS_ON, preysOn);
            put(INTERACTION_PREYED_UPON_BY, InteractType.EATEN_BY + "|" + InteractType.PREYED_UPON_BY);
            put(INTERACTION_PARASITE_OF, InteractType.PARASITE_OF.toString());
            put(INTERACTION_HAS_PARASITE, InteractType.HAS_PARASITE.toString());
            put(INTERACTION_POLLINATES, InteractType.POLLINATES.toString());
            put(INTERACTION_POLLINATED_BY, InteractType.POLLINATED_BY.toString());
            put(INTERACTION_PATHOGEN_OF, InteractType.PATHOGEN_OF.toString());
            put(INTERACTION_HAS_PATHOGEN, InteractType.HAS_PATHOGEN.toString());
            put(INTERACTION_INTERACTS_WITH, InteractType.INTERACTS_WITH.toString());
            put(INTERACTION_SYMBIONT_OF, InteractType.SYMBIONT_OF.toString());
            put(INTERACTION_HOST_OF, InteractType.HOST_OF.toString());
            put(INTERACTION_HAS_HOST, InteractType.HAS_HOST.toString());
        }
    };

    static final Map<String, String> EMPTY_PARAMS = new TreeMap<String, String>();
    public static final List<ResultField> TAXON_FIELDS = Collections.unmodifiableList(new ArrayList<ResultField>() {{
        add(TAXON_NAME);
        add(TAXON_COMMON_NAMES);
        add(TAXON_EXTERNAL_ID);
        add(TAXON_PATH);
        add(TAXON_PATH_IDS);
        add(TAXON_PATH_RANKS);
    }});

    public static final Map<ResultField, String> FIELD_MAP = Collections.unmodifiableMap(new TreeMap<ResultField, String>() {{
        put(TAXON_NAME, "taxon.name");
        put(TAXON_COMMON_NAMES, "taxon.commonNames");
        put(TAXON_EXTERNAL_ID, "taxon.externalId");
        put(TAXON_PATH, "taxon.path");
        put(TAXON_PATH_IDS, "taxon.pathIds");
        put(TAXON_PATH_RANKS, "taxon.pathNames");
    }});

    public static final ResultField[] RETURN_FIELDS_MULTI_TAXON_DEFAULT = new ResultField[]{SOURCE_TAXON_EXTERNAL_ID, SOURCE_TAXON_NAME, SOURCE_TAXON_PATH, SOURCE_SPECIMEN_LIFE_STAGE,
            INTERACTION_TYPE,
            TARGET_TAXON_EXTERNAL_ID, TARGET_TAXON_NAME, TARGET_TAXON_PATH, TARGET_SPECIMEN_LIFE_STAGE,
            LATITUDE, LONGITUDE, STUDY_TITLE};

    public static final ResultField[] RETURN_FIELDS_SINGLE_TAXON_DEFAULT = new ResultField[]{SOURCE_TAXON_NAME, INTERACTION_TYPE, TARGET_TAXON_NAME,
            LATITUDE, LONGITUDE, ALTITUDE, STUDY_TITLE, COLLECTION_TIME_IN_UNIX_EPOCH,
            SOURCE_SPECIMEN_ID,
            TARGET_SPECIMEN_ID,
            SOURCE_SPECIMEN_LIFE_STAGE,
            TARGET_SPECIMEN_LIFE_STAGE,
            SOURCE_SPECIMEN_BODY_PART,
            TARGET_SPECIMEN_BODY_PART,
            SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE,
            TARGET_SPECIMEN_PHYSIOLOGICAL_STATE,
            TARGET_SPECIMEN_TOTAL_COUNT,
            TARGET_SPECIMEN_TOTAL_VOLUME_ML,
            TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE
    };
    public static final long DEFAULT_LIMIT = 1024L;

    static public CypherQuery createDistinctTaxaInLocationQuery(Map<String, String[]> params) {
        StringBuilder builder = new StringBuilder();
        List<String> interactionTypes = collectParamValues(params, "interactionType");

        if (RequestHelper.isSpatialSearch(params)) {
            appendSpatialStartWhereWith(params, builder);
        } else {
            builder.append("START taxon = node:taxons('*:*') ");
        }

        if (RequestHelper.isSpatialSearch(params)) {
            builder.append("MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc");
            if (!interactionTypes.isEmpty()) {
                builder.append(", taxon-[:");
                builder.append(createInteractionTypeSelector(interactionTypes));
                builder.append("]->otherTaxon ");
            } else {
                builder.append(" ");
            }
        } else {
            if (!interactionTypes.isEmpty()) {
                builder.append("MATCH taxon-[:");
                builder.append(createInteractionTypeSelector(interactionTypes));
                builder.append("]->otherTaxon ");
            }
        }

        List<String> fields = collectParamValues(params, "field");
        List<ResultField> returnFields = actualReturnFields(fields, TAXON_FIELDS, TAXON_FIELDS);

        for (int i = 0; i < returnFields.size(); i++) {
            ResultField fieldName = returnFields.get(i);
            if (i == 0) {
                builder.append("RETURN distinct(").append(FIELD_MAP.get(fieldName)).append("?) as ").append(fieldName);
            } else {
                builder.append(", ").append(FIELD_MAP.get(fieldName)).append("? as ").append(fieldName);
            }
        }
        return new CypherQuery(builder.toString(), new HashMap<String, String>());
    }

    public enum QueryType {
        SINGLE_TAXON_DISTINCT, SINGLE_TAXON_ALL, MULTI_TAXON_DISTINCT, MULTI_TAXON_ALL
    }

    public static void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        if (parameterMap != null) {
            RequestHelper.appendSpatialClauses(parameterMap, query);
        }
    }

    public static String lucenePathQuery(List<String> taxonNames) {
        int count = 0;
        StringBuilder lucenePathQuery = new StringBuilder();
        for (String taxonName : taxonNames) {
            if (count > 0) {
                lucenePathQuery.append(" OR ");
            }
            lucenePathQuery.append("path:\\\"").append(taxonName).append("\\\"");
            count++;
        }
        return lucenePathQuery.toString();
    }

    public static String regex(List<String> terms) {
        List<String> quotedTerms = new ArrayList<String>();
        for (String term : terms) {
            quotedTerms.add(Pattern.quote(term).replace("\\Q", "\\\\Q").replace("\\E", "\\\\E"));
        }
        String joined = StringUtils.join(quotedTerms, "|");
        return ".*(" + joined + ").*";
    }

    private static Map<String, String> getParams(List<String> sourceTaxonNames, List<String> targetTaxonNames, List<String> accordingTo) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonNames != null && sourceTaxonNames.size() > 0) {
            paramMap.put(SOURCE_TAXON_NAME.getLabel(), lucenePathQuery(sourceTaxonNames));
        }

        if (targetTaxonNames != null && targetTaxonNames.size() > 0) {
            paramMap.put(TARGET_TAXON_NAME.getLabel(), lucenePathQuery(targetTaxonNames));
        }

        if (accordingTo != null && accordingTo.size() > 0) {
            paramMap.put("accordingTo", regex(accordingTo));
        }

        return paramMap;
    }

    static void appendTaxonSelectors(boolean includeSourceTaxon, boolean includeTargetTaxon, StringBuilder query) {
        if (includeSourceTaxon) {
            final String sourceTaxonSelector = "sourceTaxon = " + getTaxonPathSelector(SOURCE_TAXON_NAME.getLabel());
            query.append(sourceTaxonSelector);
        }
        if (includeTargetTaxon) {
            if (includeSourceTaxon) {
                query.append(", ");
            }
            final String targetTaxonSelector = "targetTaxon = " + getTaxonPathSelector(TARGET_TAXON_NAME.getLabel());
            query.append(targetTaxonSelector);
        }
    }

    private static String getTaxonPathSelector(String taxonParamName) {
        return "node:taxonPaths({" + taxonParamName + "})";
    }

    public static CypherQuery shortestPathQuery(final String startTaxon, final String endTaxon) {
        String query = "START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths ";


        HashMap<String, String> params = new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForStudy(final String studyTitle) {
        String query = "START study = node:studies(title={studyTitle}) " +
                " RETURN study.externalId? as study";

        HashMap<String, String> params = new HashMap<String, String>() {{
            put("studyTitle", studyTitle);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForTaxon(final String taxonName) {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId? as externalId";

        HashMap<String, String> taxonName1 = new HashMap<String, String>() {{
            put("taxonName", taxonName);
        }};

        return new CypherQuery(query, taxonName1);
    }

    public static CypherQuery references(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " AND study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " WHERE not(has(interact." + PropertyAndValueDictionary.INVERTED + "))" + whereClause +
                " RETURN study.institution?, study.period?, study.description?, study.contributor?, count(interact), count(distinct(sourceTaxon.name)), count(distinct(targetTaxon.name)), study.title, study.citation?, study.doi?, study.source, study.externalId?";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery locations() {
        String query = "START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude";
        return new CypherQuery(query);
    }

    public static CypherQuery buildInteractionQuery(final String sourceTaxonName, final String interactionType, final String targetTaxonName, Map parameterMap, QueryType queryType) {
        List<String> sourceTaxa = new ArrayList<String>() {{
            if (sourceTaxonName != null) {
                add(sourceTaxonName);
            }
        }};
        List<String> targetTaxa = new ArrayList<String>() {{
            if (targetTaxonName != null) {
                add(targetTaxonName);
            }
        }};
        return buildInteractionQuery(sourceTaxa, interactionType, targetTaxa, parameterMap, queryType);
    }

    public static CypherQuery buildInteractionQuery(List<String> sourceTaxonName, final String interactionType, List<String> targetTaxonName, Map parameterMap, QueryType queryType) {
        List<String> interactionTypes = new ArrayList<String>() {{
            add(interactionType);
        }};
        return interactionObservations(sourceTaxonName, interactionTypes, targetTaxonName, parameterMap, queryType);
    }

    protected static CypherQuery interactionObservations(List<String> sourceTaxa, List<String> interactionTypes, List<String> targetTaxa, Map parameterMap, QueryType queryType) {
        StringBuilder query = appendStartMatchWhereClauses(sourceTaxa, interactionTypes, targetTaxa, parameterMap);
        appendReturnClause(interactionTypes, query, queryType, collectParamValues(parameterMap, "field"));
        return new CypherQuery(query.toString(), getParams(sourceTaxa, targetTaxa, collectParamValues(parameterMap, "accordingTo")));
    }


    public static CypherQuery buildInteractionTypeQuery(Map parameterMap) {
        final List<String> taxa = collectParamValues(parameterMap, TAXON_HTTP_PARAM_NAME);
        String query = "START taxon = " + getTaxonPathSelector(TAXON_NAME.getLabel())
                + " MATCH taxon-[rel:" + InteractUtil.allInteractionsCypherClause() + "]->otherTaxon RETURN distinct(type(rel)) as " + INTERACTION_TYPE;
        return new CypherQuery(query
                , new HashMap<String, String>() {
            {
                put(TAXON_NAME.getLabel(), lucenePathQuery(taxa));
            }
        });
    }


    public static CypherQuery buildInteractionQuery(Map parameterMap, QueryType queryType) {
        List<String> sourceTaxa = collectParamValues(parameterMap, SOURCE_TAXON_HTTP_PARAM_NAME);
        List<String> targetTaxa = collectParamValues(parameterMap, TARGET_TAXON_HTTP_PARAM_NAME);
        List<String> interactionTypeSelectors = collectParamValues(parameterMap, "interactionType");
        return interactionObservations(sourceTaxa, interactionTypeSelectors, targetTaxa, parameterMap, queryType);
    }


    private static void appendReturnClause(final List<String> interactionType, StringBuilder query, QueryType returnType, List<String> requestedReturnFields) {
        switch (returnType) {
            case SINGLE_TAXON_DISTINCT:
                appendReturnClauseDistinct(interactionType.get(0), query, Arrays.asList(SOURCE_TAXON_NAME, INTERACTION_TYPE, TARGET_TAXON_NAME));
                break;
            case SINGLE_TAXON_ALL:
                HashMap<ResultField, String> selectors = new HashMap<ResultField, String>(defaultSelectors()) {
                    {
                        put(INTERACTION_TYPE, "'" + interactionType.get(0) + "'");
                        put(STUDY_TITLE, "study." + Study.TITLE);
                        put(ResultField.STUDY_URL, "study.externalId?");
                        put(ResultField.STUDY_DOI, "study.doi?");
                        put(ResultField.STUDY_CITATION, "study.citation?");
                        put(ResultField.STUDY_SOURCE_CITATION, "study.source?");
                        put(COLLECTION_TIME_IN_UNIX_EPOCH, "collected_rel.dateInUnixEpoch?");
                        put(SOURCE_SPECIMEN_ID, "ID(sourceSpecimen)");
                        put(TARGET_SPECIMEN_ID, "ID(targetSpecimen)");
                        put(TARGET_SPECIMEN_TOTAL_COUNT, "targetSpecimen." + Specimen.TOTAL_COUNT + "?");
                        put(TARGET_SPECIMEN_TOTAL_VOLUME_ML, "targetSpecimen." + Specimen.TOTAL_VOLUME_IN_ML + "?");
                        put(TARGET_SPECIMEN_TOTAL_FREQUENCY_OF_OCCURRENCE, "targetSpecimen." + Specimen.FREQUENCY_OF_OCCURRENCE + "?");
                    }
                };
                appendReturnClause(query, actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_SINGLE_TAXON_DEFAULT), selectors.keySet()), selectors);
                break;
            case MULTI_TAXON_ALL:
                selectors = new HashMap<ResultField, String>(defaultSelectors()) {
                    {
                        put(SOURCE_SPECIMEN_LIFE_STAGE, "sourceSpecimen.lifeStage?");
                        StringBuilder interactionBuilder = new StringBuilder();
                        appendInteractionTypeReturn(interactionBuilder, "type(interaction)");
                        put(INTERACTION_TYPE, interactionBuilder.toString());
                        put(TARGET_SPECIMEN_LIFE_STAGE, "targetSpecimen.lifeStage?");
                        put(STUDY_TITLE, "study.title");
                        put(ResultField.STUDY_URL, "study.externalId?");
                        put(ResultField.STUDY_DOI, "study.doi?");
                        put(ResultField.STUDY_CITATION, "study.citation?");
                        put(ResultField.STUDY_SOURCE_CITATION, "study.source?");
                    }
                };
                appendReturnClausez(query, actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_MULTI_TAXON_DEFAULT), selectors.keySet()), selectors);
                break;
            case MULTI_TAXON_DISTINCT:
                selectors = new HashMap<ResultField, String>(defaultSelectors()) {
                    {
                        put(SOURCE_TAXON_EXTERNAL_ID, "sTaxon.externalId?");
                        put(SOURCE_TAXON_NAME, "sTaxon.name");
                        put(SOURCE_TAXON_PATH, "sTaxon.path?");
                        put(SOURCE_SPECIMEN_LIFE_STAGE, "NULL");
                        put(INTERACTION_TYPE, appendInteractionTypeReturn(new StringBuilder(), "iType").toString());
                        put(TARGET_TAXON_EXTERNAL_ID, "tTaxon.externalId?");
                        put(TARGET_TAXON_NAME, "tTaxon.name");
                        put(TARGET_TAXON_PATH, "tTaxon.path?");
                        put(TARGET_SPECIMEN_LIFE_STAGE, "NULL");
                        put(LATITUDE, "NULL");
                        put(LONGITUDE, "NULL");
                        put(STUDY_TITLE, "NULL");
                        put(ResultField.STUDY_URL, "NULL");
                        put(ResultField.STUDY_DOI, "NULL");
                        put(ResultField.STUDY_CITATION, "NULL");
                        put(ResultField.STUDY_SOURCE_CITATION, "study.source?");
                    }
                };
                List<ResultField> returnFields = actualReturnFields(requestedReturnFields, Arrays.asList(RETURN_FIELDS_MULTI_TAXON_DEFAULT), selectors.keySet());
                appendReturnClauseDistinctz(query, returnFields, selectors);
                break;
            default:
                throw new IllegalArgumentException("invalid option [" + returnType + "]");
        }
    }

    protected static List<ResultField> actualReturnFields(List<String> requestedReturnFields, List<ResultField> defaultReturnFields, Collection<ResultField> availableReturnFields) {
        List<ResultField> returnFields = new ArrayList<ResultField>();
        for (String requestedReturnField : requestedReturnFields) {
            for (ResultField resultField : ResultField.values()) {
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


    protected static StringBuilder appendTaxonWherClauseIfNecessary(Map parameterMap, List<String> sourceTaxa, List<String> targetTaxa, StringBuilder query) {
        boolean spatialSearch = RequestHelper.isSpatialSearch(parameterMap);
        if (collectParamValues(parameterMap, "accordingTo").size() > 0) {
            appendAndOrWhere(targetTaxa, query, spatialSearch);
            if (targetTaxa.size() > 0) {
                query.append("(");
            }
            appendTaxonSelector(query, "targetTaxon", targetTaxa);

            appendAndOrWhere(sourceTaxa, query, spatialSearch || targetTaxa.size() > 0);
            if (targetTaxa.size() == 0 && sourceTaxa.size() > 0) {
                query.append("(");
            }
            appendTaxonSelector(query, "sourceTaxon", sourceTaxa);
            if (sourceTaxa.size() > 0 || targetTaxa.size() > 0) {
                query.append(") ");
            }
        } else if (sourceTaxa.size() > 0) {
            appendAndOrWhere(targetTaxa, query, spatialSearch);
            appendTaxonSelector(query, "targetTaxon", targetTaxa);
        }
        return query;
    }

    private static void appendAndOrWhere(List<String> taxa, StringBuilder query, boolean hasWhereClause) {
        if (taxa.size() > 0) {
            if (hasWhereClause) {
                query.append("AND ");
            } else {
                query.append(" WHERE ");
            }
        }
    }

    protected static StringBuilder appendStartClause2(Map parameterMap, List<String> sourceTaxa, List<String> targetTaxa, StringBuilder query) {
        query.append("START");
        List<String> accordingTo = collectParamValues(parameterMap, "accordingTo");
        if (accordingTo.size() > 0) {
            query.append(" study = node:studies('*:*') WHERE (has(study.externalId) AND study.externalId =~ {accordingTo}) OR study.citation =~ {accordingTo} OR study.source =~ {accordingTo} WITH study");
        } else if (noSearchCriteria(RequestHelper.isSpatialSearch(parameterMap), sourceTaxa, targetTaxa)) {
            query.append(" study = node:studies('*:*')");
        } else if (sourceTaxa.size() == 0 && targetTaxa.size() == 0) {
            query.append(" loc = node:locations('*:*')");
        } else {
            if (sourceTaxa.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(true, false, query);
            } else if (targetTaxa.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(false, true, query);
            }
        }
        return query;
    }

    private static StringBuilder appendStartMatchWhereClauses(List<String> sourceTaxa, List<String> interactionTypes, List<String> targetTaxa, Map parameterMap) {
        StringBuilder query = new StringBuilder();
        appendStartClause2(parameterMap, sourceTaxa, targetTaxa, query);
        appendMatchAndWhereClause(interactionTypes, parameterMap, query);
        return appendTaxonWherClauseIfNecessary(parameterMap, sourceTaxa, targetTaxa, query);
    }

    protected static void appendReturnClause(StringBuilder query, List<ResultField> returnFields, Map<ResultField, String> selectors) {
        query.append(" RETURN ");
        appendReturnFields(query, returnFields, selectors);
    }

    private static Map<ResultField, String> defaultSelectors() {
        return new HashMap<ResultField, String>() {
            {
                addSourceTaxonFields();
                addTargetTaxonFields();
                put(LATITUDE, "loc." + Location.LATITUDE + "?");
                put(LATITUDE, "loc." + Location.LATITUDE + "?");
                put(LONGITUDE, "loc." + Location.LONGITUDE + "?");
                put(ALTITUDE, "loc." + Location.ALTITUDE + "?");
                put(SOURCE_SPECIMEN_LIFE_STAGE, "sourceSpecimen." + Specimen.LIFE_STAGE_LABEL + "?");
                put(TARGET_SPECIMEN_LIFE_STAGE, "targetSpecimen." + Specimen.LIFE_STAGE_LABEL + "?");
                put(SOURCE_SPECIMEN_BODY_PART, "sourceSpecimen." + Specimen.BODY_PART_LABEL + "?");
                put(TARGET_SPECIMEN_BODY_PART, "targetSpecimen." + Specimen.BODY_PART_LABEL + "?");
                put(SOURCE_SPECIMEN_PHYSIOLOGICAL_STATE, "sourceSpecimen." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "?");
                put(TARGET_SPECIMEN_PHYSIOLOGICAL_STATE, "targetSpecimen." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "?");

            }

            private void addTargetTaxonFields() {
                put(TARGET_TAXON_NAME, "targetTaxon.name");
                put(TARGET_TAXON_COMMON_NAMES, "targetTaxon.commonNames?");
                put(TARGET_TAXON_EXTERNAL_ID, "targetTaxon.externalId?");
                put(TARGET_TAXON_PATH, "targetTaxon.path?");
                put(TARGET_TAXON_PATH_RANKS, "targetTaxon.pathNames?");
                put(TARGET_TAXON_PATH_IDS, "targetTaxon.pathIds?");
            }

            private void addSourceTaxonFields() {
                put(SOURCE_TAXON_NAME, "sourceTaxon.name");
                put(SOURCE_TAXON_COMMON_NAMES, "sourceTaxon.commonNames?");
                put(SOURCE_TAXON_EXTERNAL_ID, "sourceTaxon.externalId?");
                put(SOURCE_TAXON_PATH, "sourceTaxon.path?");
                put(SOURCE_TAXON_PATH_RANKS, "sourceTaxon.pathNames?");
                put(SOURCE_TAXON_PATH_IDS, "sourceTaxon.pathIds?");
            }
        };
    }

    protected static void appendReturnClauseDistinct(final String interactionType, StringBuilder query, List<ResultField> fields) {
        Map<ResultField, String> selectors = new HashMap<ResultField, String>() {
            {
                put(SOURCE_TAXON_NAME, "sourceTaxon.name");
                put(INTERACTION_TYPE, "'" + interactionType + "'");
                put(TARGET_TAXON_NAME, "collect(distinct(targetTaxon.name))");
            }
        };
        query.append(" RETURN ");
        appendReturnFields(query, fields, selectors);
    }

    protected static StringBuilder appendMatchAndWhereClause(List<String> interactionTypes, Map parameterMap, StringBuilder query) {
        String interactionMatch = getInteractionMatch(createInteractionTypeSelector(interactionTypes));
        query.append(" ")
                .append(interactionMatch);
        addLocationClausesIfNecessary(query, parameterMap);
        return query;
    }

    public static StringBuilder appendStartClause(boolean includeSourceTaxon, boolean includeTargetTaxon, StringBuilder query) {
        query.append("START ");
        appendTaxonSelectors(includeSourceTaxon, includeTargetTaxon, query);
        return query;
    }

    private static String getInteractionMatch(String interactionTypeSelector) {
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + interactionTypeSelector + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study";
    }

    public static CypherQuery sourcesQuery() {
        String cypherQuery = "START study=node:studies('*:*')" +
                " RETURN distinct(study.source)";
        return new CypherQuery(cypherQuery, EMPTY_PARAMS);
    }


    protected static void appendReturnClausez(StringBuilder query, List<ResultField> returnFields, Map<ResultField, String> selectors) {
        query.append("RETURN ");
        appendReturnFields(query, returnFields, selectors);
    }

    protected static StringBuilder appendInteractionTypeReturn(StringBuilder query, String interactionTypeValue) {
        int terms = 0;
        StringBuilder suffix = new StringBuilder();
        for (Map.Entry<String, String> interactMap : TRANSLATION_MAP.entrySet()) {
            String externalType = interactMap.getKey();
            String[] internalTypes = interactMap.getValue().split("\\|");

            for (String internalType : internalTypes) {
                suffix.append(",'").append(internalType).append("','").append(externalType).append("')");
                terms++;
            }

        }

        query.append(StringUtils.repeat("replace(", terms));
        query.append(interactionTypeValue);
        query.append(suffix);
        return query;
    }

    protected static void appendReturnClauseDistinctz(StringBuilder query, List<ResultField> returnFields, Map<ResultField, String> selectors) {
        query.append("WITH distinct targetTaxon as tTaxon, type(interaction) as iType, sourceTaxon as sTaxon ");
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

    protected static String createInteractionTypeSelector(List<String> interactionTypeSelectors) {
        List<String> cypherTypes = new ArrayList<String>();
        for (String type : interactionTypeSelectors) {
            if (DIRECTIONAL_INTERACTION_TYPE_MAP.containsKey(type)) {
                cypherTypes.add(DIRECTIONAL_INTERACTION_TYPE_MAP.get(type));
            } else if (StringUtils.isNotBlank(type)) {
                throw new IllegalArgumentException("unsupported interaction type [" + type + "]");
            }
        }
        return cypherTypes.isEmpty() ? InteractUtil.allInteractionsCypherClause() : StringUtils.join(cypherTypes, "|");
    }

    private static boolean noSearchCriteria(boolean spatialSearch, List<String> sourceTaxaSelectors, List<String> targetTaxaSelectors) {
        return !spatialSearch && sourceTaxaSelectors.size() == 0 && targetTaxaSelectors.size() == 0;
    }

    private static void appendTaxonSelector(StringBuilder query, String taxonLabel, List<String> taxonNames) {
        if (taxonNames.size() > 0) {
            query.append("has(").append(taxonLabel).append(".path) AND ").append(taxonLabel).append(".path =~ '(.*(");
            query.append(StringUtils.join(taxonNames, "|"));
            query.append(").*)' ");
        }
    }

    protected static List<String> collectParamValues(Map parameterMap, String taxonSearchKey) {
        List<String> taxa = new ArrayList<String>();
        if (parameterMap.containsKey(taxonSearchKey)) {
            Object paramObject = parameterMap.get(taxonSearchKey);
            if (paramObject instanceof String[]) {
                Collections.addAll(taxa, (String[]) paramObject);
            } else if (paramObject instanceof String) {
                taxa.add((String) paramObject);
            }
        }
        return taxa;
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query) {
        return createPagedQuery(request, query, DEFAULT_LIMIT);
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query, long defaultLimit) {
        long defaultValue = 0L;
        long offset = getValueOrDefault(request, "offset", defaultValue);
        if (offset == defaultValue) {
            offset = getValueOrDefault(request, "skip", defaultValue);
        }
        long limit = getValueOrDefault(request, "limit", defaultLimit);
        return new CypherQuery(query.getQuery() + " SKIP " + offset + " LIMIT " + limit, query.getParams());
    }

    private static long getValueOrDefault(HttpServletRequest request, String paramName, long defaultValue) {
        long offset = defaultValue;
        if (request != null) {
            String offsetValue = request.getParameter(paramName);
            if (org.apache.commons.lang.StringUtils.isNotBlank(offsetValue)) {
                try {
                    offset = Long.parseLong(offsetValue);
                } catch (NumberFormatException ex) {
                    LOG.warn("malformed " + paramName + " found [" + offsetValue + "]", ex);
                }
            }
        }
        return offset;
    }

    public static CypherQuery spatialInfo(Map<String, String[]> parameterMap) {
        final String interactionLabel = "sourceTaxon.name + type(interact) + targetTaxon.name";
        StringBuilder query = new StringBuilder();

        if (RequestHelper.isSpatialSearch(parameterMap)) {
            appendSpatialStartWhereWith(parameterMap, query);
        } else {
            query.append("START study = node:studies('*:*') ");
        }

        query.append("MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study")
                .append(", sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon");
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", sourceSpecimen-[:COLLECTED_AT]->loc");
        }
        query.append(" WHERE not(has(interact.");
        query.append(PropertyAndValueDictionary.INVERTED);
        query.append("))");

        Map<String, String> params = addSourceWhereClause(parameterMap, query);

        query.append(" RETURN count(distinct(study)) as `number of distinct studies`")
                .append(", count(interact) as `number of interactions`")
                .append(", count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`")
                .append(", count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`")
                .append(", count(distinct(study.source)) as `number of distinct study sources`")
                .append(", count(c." + Specimen.DATE_IN_UNIX_EPOCH + "?) as `number of interactions with timestamp`")
        ;
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", count(distinct(loc))");
        } else {
            query.append(", NULL");
        }
        query.append(" as `number of distinct locations`")
                .append(", count(distinct(").append(interactionLabel).append(")) as `number of distinct interactions`");
        return new CypherQuery(query.toString(), params);
    }

    public static void appendSpatialStartWhereWith(Map<String, String[]> parameterMap, StringBuilder query) {
        query.append("START loc = node:locations('*:*') WHERE ");
        RequestHelper.addSpatialWhereClause(RequestHelper.parseSpatialSearchParams(parameterMap), query);
        query.append("WITH loc ");
    }

    protected static Map<String, String> addSourceWhereClause(Map<String, String[]> parameterMap, StringBuilder query) {
        String[] sourceList = parameterMap == null ? null : parameterMap.get("source");
        final String source = sourceList != null && sourceList.length > 0 ? sourceList[0] : null;
        String sourceWhereClause = StringUtils.isBlank(source) ? "" : " study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        if (StringUtils.isNotBlank(sourceWhereClause)) {
            query.append(" AND");
            query.append(sourceWhereClause);
        }
        return params;
    }

    public static CypherQuery stats(final String source) {
        HashMap<String, String[]> paramMap = new HashMap<String, String[]>();
        if (StringUtils.isNotBlank(source)) {
            paramMap.put("source", new String[]{source});
        }
        return spatialInfo(paramMap);
    }
}
