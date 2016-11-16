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
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
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

public class GlobalNamesService implements PropertyEnricher {
    private static final Log LOG = LogFactory.getLog(GlobalNamesService.class);

    private final GlobalNamesSources source;
    private boolean includeCommonNames = false;

    public GlobalNamesService() {
        this(GlobalNamesSources.ITIS);
    }

    public GlobalNamesService(GlobalNamesSources source) {
        super();
        this.source = source;
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
            public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {
                if (NameType.SAME_AS.equals(nameType)) {
                    exactMatches.add(taxon);
                } else if (NameType.SYNONYM_OF.equals(nameType)) {
                    synonyms.add(taxon);
                }
            }
        }, Collections.singletonList(source));

        if (exactMatches.size() > 0) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(exactMatches.get(0)));
        } else if (synonyms.size() == 1) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(synonyms.get(0)));
        }

        return Collections.unmodifiableMap(enrichedProperties);
    }

    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener, List<GlobalNamesSources> sources) throws PropertyEnricherException {
        if (names.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }

        try {
            String result = queryForNames(names, sources);
            parseResult(termMatchListener, result);
        } catch (IOException e) {
            throw new PropertyEnricherException("Failed to query", e);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("Failed to query", e);
        }

    }

    protected String queryForNames(List<String> names, List<GlobalNamesSources> sources) throws URISyntaxException, IOException {
        List<String> sourceIds = new ArrayList<String>();
        for (GlobalNamesSources globalNamesSources : sources) {
            sourceIds.add(Integer.toString(globalNamesSources.getId()));
        }

        String query = "data_source_ids=" + StringUtils.join(sourceIds, "|");
        if (shouldIncludeCommonNames()) {
            query = "with_vernaculars=true&" + query;
        }

        HttpClient httpClient = HttpUtil.getHttpClient();
        URI uri = new URI("http", "resolver.globalnames.org"
                , "/name_resolvers.json"
                , query
                , null);
        HttpPost post = new HttpPost(uri);

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("data", StringUtils.join(names, "\n")));
        post.setEntity(new UrlEncodedFormEntity(params, CharsetConstant.UTF8));

        return httpClient.execute(post, new BasicResponseHandler());
    }

    protected void parseResult(TermMatchListener termMatchListener, String result) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(result);
        JsonNode dataList = jsonNode.get("data");
        if (dataList != null && dataList.isArray()) {
            for (JsonNode data : dataList) {
                JsonNode results = data.get("results");
                if (results != null && results.isArray()) {
                    for (JsonNode aResult : results) {
                        Taxon taxon = new TaxonImpl();
                        TaxonomyProvider provider = getTaxonomyProvider(aResult);
                        if (provider == null) {
                            LOG.warn("found unsupported data_source_id");
                        } else {
                            if (aResult.has("classification_path")
                                    && aResult.has("classification_path_ranks")) {
                                parseClassification(termMatchListener, data, aResult, taxon, provider);
                            }
                        }
                    }
                }
            }
        }
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

    protected void parseClassification(TermMatchListener termMatchListener, JsonNode data, JsonNode aResult, Taxon taxon, TaxonomyProvider provider) {
        String classificationPath = aResult.get("classification_path").asText();
        taxon.setPath(parseList(classificationPath));

        if (aResult.has("classification_path_ids")) {
            String classificationPathIds = aResult.get("classification_path_ids").asText();
            taxon.setPathIds(parseList(classificationPathIds, provider.getIdPrefix()));
        }
        String pathRanks = aResult.get("classification_path_ranks").asText();
        taxon.setPathNames(parseList(pathRanks));
        String[] ranks = pathRanks.split("\\|");
        if (ranks.length > 0) {
            String rank = ranks[ranks.length - 1];
            taxon.setRank(rank);
        }
        String[] taxonNames = classificationPath.split("\\|");
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
            Long suppliedId = data.has("supplied_id") ? data.get("supplied_id").asLong() : null;
            String suppliedNameString = data.get("supplied_name_string").getTextValue();

            boolean isExactMatch = aResult.has("match_type")
                    && aResult.get("match_type").getIntValue() < 3;

            NameType nameType = isExactMatch ? NameType.SAME_AS : NameType.SIMILAR_TO;
            if (isExactMatch && aResult.has("current_name_string")) {
                nameType = NameType.SYNONYM_OF;
            }
            termMatchListener.foundTaxonForName(suppliedId, suppliedNameString, taxon, nameType);
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

    public GlobalNamesSources getSource() {
        return source;
    }

    public void shutdown() {

    }
}
