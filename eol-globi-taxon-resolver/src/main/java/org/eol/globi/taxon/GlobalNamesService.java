package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalNamesService implements PropertyEnricher, TermMatcher {
    private static final Log LOG = LogFactory.getLog(GlobalNamesService.class);
    public static final List<Integer> MATCH_TYPES_EXACT = Arrays.asList(1, 2, 6);

    private final List<GlobalNamesSources> sources;
    private boolean includeCommonNames = false;

    public GlobalNamesService() {
        this(GlobalNamesSources.ITIS);
    }

    public GlobalNamesService(GlobalNamesSources source) {
        this(Collections.singletonList(source));
    }

    public GlobalNamesService(List<GlobalNamesSources> sources) {
        super();
        this.sources = sources;
    }

    public void setIncludeCommonNames(boolean includeCommonNames) {
        this.includeCommonNames = includeCommonNames;
    }

    public boolean shouldIncludeCommonNames() {
        return this.includeCommonNames;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>();
        final List<Taxon> exactMatches = new ArrayList<Taxon>();
        final List<Taxon> synonyms = new ArrayList<Taxon>();
        findTermsForNames(Collections.singletonList(properties.get(PropertyAndValueDictionary.NAME)), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType) {
                if (NameType.SAME_AS.equals(nameType)) {
                    exactMatches.add(taxon);
                } else if (NameType.SYNONYM_OF.equals(nameType)) {
                    synonyms.add(taxon);
                }
            }
        });

        if (exactMatches.size() > 0) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(exactMatches.get(0)));
        } else if (synonyms.size() == 1) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(synonyms.get(0)));
        }

        return Collections.unmodifiableMap(enrichedProperties);
    }

    @Override
    public void findTerms(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        if (terms.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }
        findTermsForNames(terms.stream().map(Term::getName).collect(Collectors.toList()), termMatchListener);
    }

    @Override
    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws PropertyEnricherException {
        if (names.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }

        try {
            URI uri = buildPostRequestURI(sources);
            try {
                parseResult(termMatchListener, executeQuery(names, uri));
            } catch (IOException e) {
                if (names.size() > 1) {
                    LOG.warn("retrying names query one name at a time: failed to perform batch query", e);
                    List<String> namesFailed = new ArrayList<>();
                    for (String name : names) {
                        try {
                            parseResult(termMatchListener, executeQuery(Collections.singletonList(name), uri));
                        } catch (IOException e1) {
                            namesFailed.add(name);
                        }
                    }
                    if (namesFailed.size() > 0) {
                        throw new PropertyEnricherException("Failed to execute individual name queries for [" + StringUtils.join(namesFailed, "|") + "]");
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("Failed to query", e);
        }

    }

    private String executeQuery(List<String> names, URI uri) throws IOException {
        HttpClient httpClient = HttpUtil.getHttpClient();
        HttpPost post = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("data", StringUtils.join(names, "\n")));
        post.setEntity(new UrlEncodedFormEntity(params, CharsetConstant.UTF8));

        return httpClient.execute(post, new BasicResponseHandler());
    }

    private URI buildPostRequestURI(List<GlobalNamesSources> sources) throws URISyntaxException {
        List<String> sourceIds = new ArrayList<String>();
        for (GlobalNamesSources globalNamesSources : sources) {
            sourceIds.add(Integer.toString(globalNamesSources.getId()));
        }

        String query = "data_source_ids=" + StringUtils.join(sourceIds, "|");
        if (shouldIncludeCommonNames()) {
            query = "with_vernaculars=true&" + query;
        }

        return new URI("http", "resolver.globalnames.org"
                , "/name_resolvers.json"
                , query
                , null);
    }

    private void parseResult(TermMatchListener termMatchListener, String result) throws PropertyEnricherException {
        try {
            parseResultNode(termMatchListener, new ObjectMapper().readTree(result));
        } catch (IOException ex) {
            throw new PropertyEnricherException("failed to parse json string [" + result + "]", ex);
        }
    }

    private void parseResultNode(TermMatchListener termMatchListener, JsonNode jsonNode) {
        JsonNode dataList = jsonNode.get("data");
        if (dataList != null && dataList.isArray()) {
            for (JsonNode data : dataList) {
                JsonNode results = data.get("results");
                if (results == null) {
                    if (dataList.size() > 0) {
                        JsonNode firstDataElement = dataList.get(0);
                        firstDataElement.get("supplied_name_string");
                        if (firstDataElement.has("is_known_name")
                                && firstDataElement.has("supplied_name_string")
                                && !firstDataElement.get("is_known_name").asBoolean(false)) {
                            noMatch(termMatchListener, data);
                        }
                    }
                } else if (results.isArray()) {
                    for (JsonNode aResult : results) {
                        TaxonomyProvider provider = getTaxonomyProvider(aResult);
                        if (provider == null) {
                            LOG.warn("found unsupported data_source_id");
                        } else {
                            if (aResult.has("classification_path")
                                    && aResult.has("classification_path_ranks")) {
                                parseClassification(termMatchListener, data, aResult, provider);
                            } else {
                                noMatch(termMatchListener, data);
                            }
                        }
                    }
                }
            }
        }
    }

    private void noMatch(TermMatchListener termMatchListener, JsonNode data) {
        String suppliedNameString = getSuppliedNameString(data);
        termMatchListener.foundTaxonForName(requestId(data), suppliedNameString, new TaxonImpl(suppliedNameString), NameType.NONE);
    }

    private String parseList(String list) {
        return parseList(list, null);
    }

    private String parseList(String list, String prefix) {
        String[] split = StringUtils.splitPreserveAllTokens(list, "|");
        List<String> parsedList = Collections.emptyList();
        if (split != null) {
            parsedList = Arrays.asList(split);
        }
        if (StringUtils.isNotBlank(prefix)) {
            List<String> prefixed = new ArrayList<String>();
            for (String s : parsedList) {
                if (StringUtils.startsWith(s, "gn:")) {
                    prefixed.add("");
                } else {
                    prefixed.add(prefix + s);
                }
            }
            parsedList = prefixed;
        }
        return StringUtils.join(parsedList, CharsetConstant.SEPARATOR);
    }

    public static boolean pathTailRepetitions(Taxon taxon) {
        boolean repetitions = false;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(taxon.getPath())) {
            String[] split = org.apache.commons.lang3.StringUtils.split(taxon.getPath(), CharsetConstant.SEPARATOR_CHAR);
            if (split.length > 2
                    && repeatInTail(split)) {
                repetitions = true;
            }
        }
        return repetitions;
    }

    private static boolean repeatInTail(String[] split) {
        String last = org.apache.commons.lang3.StringUtils.trim(split[split.length - 1]);
        String secondToLast = org.apache.commons.lang3.StringUtils.trim(split[split.length - 2]);
        return org.apache.commons.lang3.StringUtils.equals(last, secondToLast);
    }


    protected void parseClassification(TermMatchListener termMatchListener, JsonNode data, JsonNode aResult, TaxonomyProvider provider) {
        Taxon taxon = new TaxonImpl();
        String classificationPath = aResult.get("classification_path").asText();
        taxon.setPath(parseList(classificationPath));

        if (aResult.has("classification_path_ids")) {
            String classificationPathIds = aResult.get("classification_path_ids").asText();
            taxon.setPathIds(parseList(classificationPathIds, provider.getIdPrefix()));
        }
        String pathRanks = aResult.get("classification_path_ranks").asText();
        taxon.setPathNames(parseList(pathRanks));
        String[] ranks = CSVTSVUtil.splitPipes(pathRanks);
        if (ranks.length > 0) {
            String rank = ranks[ranks.length - 1];
            taxon.setRank(rank);
        }
        String[] taxonNames = CSVTSVUtil.splitPipes(classificationPath);
        if (ranks.length > 0 && taxonNames.length > 0) {
            String taxonName = taxonNames[taxonNames.length - 1];
            taxon.setName(taxonName);
        } else {
            taxon.setName(aResult.get("canonical_form").asText());
        }

        String taxonIdLabel = aResult.has("current_taxon_id") ? "current_taxon_id" : "taxon_id";
        String taxonIdValue = aResult.get(taxonIdLabel).asText();
        // see https://github.com/GlobalNamesArchitecture/gni/issues/35
        if (!StringUtils.startsWith(taxonIdValue, "gn:")) {
            String externalId = provider.getIdPrefix() + taxonIdValue;
            taxon.setExternalId(externalId);
            String suppliedNameString = getSuppliedNameString(data);

            boolean isExactMatch = aResult.has("match_type")
                    && MATCH_TYPES_EXACT.contains(aResult.get("match_type").getIntValue());

            NameType nameType = isExactMatch ? NameType.SAME_AS : NameType.SIMILAR_TO;
            if (isExactMatch && aResult.has("current_name_string")) {
                nameType = NameType.SYNONYM_OF;
            }

            // related to https://github.com/GlobalNamesArchitecture/gni/issues/48
            if (!pathTailRepetitions(taxon)) {
                termMatchListener.foundTaxonForName(requestId(data), suppliedNameString, taxon, nameType);
            }
        }

        if (aResult.has("vernaculars")) {
            List<String> commonNames = new ArrayList<String>();
            JsonNode vernaculars = aResult.get("vernaculars");
            for (JsonNode vernacular : vernaculars) {
                if (vernacular.has("name") && vernacular.has("language")) {
                    String name = vernacular.get("name").asText();
                    String language = vernacular.get("language").asText();
                    if (!StringUtils.equals(name, "null") && !StringUtils.equals(language, "null")) {
                        commonNames.add(vernacular.get("name").asText() + " @" + language);
                    }
                }
            }
            if (commonNames.size() > 0) {
                taxon.setCommonNames(StringUtils.join(commonNames, CharsetConstant.SEPARATOR));
            }
        }
    }

    private String getSuppliedNameString(JsonNode data) {
        return data.get("supplied_name_string").getTextValue();
    }

    private Long requestId(JsonNode data) {
        return data.has("supplied_id") ? data.get("supplied_id").asLong() : null;
    }

    private TaxonomyProvider getTaxonomyProvider(JsonNode aResult) {
        TaxonomyProvider provider = null;
        if (aResult.has("data_source_id")) {
            int sourceId = aResult.get("data_source_id").getIntValue();

            GlobalNamesSources[] values = GlobalNamesSources.values();
            for (GlobalNamesSources value : values) {
                if (value.getId() == sourceId) {
                    provider = value.getProvider();
                    break;
                }
            }
        }
        return provider;
    }

    public List<GlobalNamesSources> getSources() {
        return sources;
    }

    public void shutdown() {

    }
}
