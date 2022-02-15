package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionProcessorAbstract;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_RANK;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_RANK;

public class INaturalistOccurrenceIdIdEnricher extends InteractionProcessorAbstract {

    private final ResourceService resourceService;

    public INaturalistOccurrenceIdIdEnricher(InteractionListener listener, ImportLogger logger, ResourceService resourceService) {
        super(listener, logger);
        this.resourceService = resourceService;
    }

    private static final String OBSERVATION_URL_PREFIX
            = "http[s]{0,1}://(www\\.){0,1}inaturalist.org/observations/";

    private static final Pattern INATURALIST_OBSERVATION_MATCHER
            = Pattern.compile(OBSERVATION_URL_PREFIX + "([0-9]+).*");

    static void enrichWithINaturalistObservation(InputStream is,
                                                 String taxonNameField,
                                                 String taxonIdField,
                                                 String taxonRankField,
                                                 Map<String, String> properties) throws IOException {

        if (is != null) {
            JsonNode jsonNode =
                    new ObjectMapper().readTree(is);

            if (jsonNode.has("taxon")) {
                JsonNode taxonNode = jsonNode.get("taxon");

                if (taxonNode.has("id")) {
                    properties.putIfAbsent(taxonIdField, TaxonomyProvider.INATURALIST_TAXON.getIdPrefix() + taxonNode.get("id").asText());
                }

                if (taxonNode.has("name")) {
                    properties.putIfAbsent(taxonNameField, taxonNode.get("name").asText());
                }

                if (taxonNode.has("rank")) {
                    properties.putIfAbsent(taxonRankField, taxonNode.get("rank").asText());
                }
            }

            if (jsonNode.has("observed_on")) {
                properties.putIfAbsent(DatasetImporterForMetaTable.EVENT_DATE, jsonNode.get("observed_on").asText());
            }

            if (jsonNode.has("latitude")) {
                properties.putIfAbsent(DatasetImporterForTSV.DECIMAL_LATITUDE, jsonNode.get("latitude").asText());
            }

            if (jsonNode.has("longitude")) {
                properties.putIfAbsent(DatasetImporterForTSV.DECIMAL_LONGITUDE, jsonNode.get("longitude").asText());
            }

            if (jsonNode.has("place_guess")) {
                properties.putIfAbsent(LOCALITY_NAME, jsonNode.get("place_guess").asText());
            }
        }

    }

    private InputStream getResponse(String id) throws IOException {
        return resourceService.retrieve(URI.create(id));

    }


    private static String parseObservationId(String id) {
        Matcher matcher = INATURALIST_OBSERVATION_MATCHER.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalStateException("failed to match [" + id + "]");
        }
        return matcher.group(2);
    }

    private static boolean isINaturalistObservation(String id) {
        return StringUtils.isNoneBlank(id) &&
                INATURALIST_OBSERVATION_MATCHER.matcher(id).matches();
    }

    public Map<String, String> enrich(final Map<String, String> properties) throws StudyImporterException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);

        enrichFields(enrichedProperties, SOURCE_TAXON_NAME, SOURCE_TAXON_ID, SOURCE_TAXON_RANK, "sourceOccurrenceId");
        enrichFields(enrichedProperties, TARGET_TAXON_NAME, TARGET_TAXON_ID, TARGET_TAXON_RANK, "targetOccurrenceId");

        return Collections.unmodifiableMap(enrichedProperties);
    }

    private void enrichFields(Map<String, String> enrichedProperties, String taxonNameField, String taxonIdField, String taxonRankField, String occurrenceIdField) throws StudyImporterException {
        String occurrenceId = enrichedProperties.get(occurrenceIdField);
        if (isINaturalistObservation(occurrenceId)) {
            try (InputStream is = getResponse(createObservationUrl(occurrenceId))) {
                enrichWithINaturalistObservation(is,
                        taxonNameField,
                        taxonIdField,
                        taxonRankField,
                        enrichedProperties);
            } catch (IOException e) {
                throw new StudyImporterException("failed to resolve [" + occurrenceId + "]");
            }
        }
    }

    public String createObservationUrl(String sourceOccurrenceId) {
        return "https://www.inaturalist.org/observations/" + parseObservationId(sourceOccurrenceId) + ".json";
    }


    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        emit(enrich(interaction));
    }
}
