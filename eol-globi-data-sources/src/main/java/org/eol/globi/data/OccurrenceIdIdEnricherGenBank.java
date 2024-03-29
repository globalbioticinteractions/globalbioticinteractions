package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionProcessorAbstract;
import org.eol.globi.service.ResourceService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_BODY_PART_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;

public class OccurrenceIdIdEnricherGenBank extends InteractionProcessorAbstract {

    private final ResourceService resourceService;
    public static final Pattern PATTERN_HOST = Pattern.compile("\\s+/host=\"([^\"]+)\".*");
    public static final Pattern PATTERN_ORGANISM = Pattern.compile("\\s+/organism=\"([^\"]+)\".*");
    public static final Pattern PATTERN_ISOLATION_SOURCE = Pattern.compile("\\s+/isolation_source=\"([^\"]+)\".*");
    public static final Pattern PATTERN_TAXON_ID = Pattern.compile("\\s+/db_xref=\"taxon:([^\"]+)\".*");
    public static final Pattern PATTERN_COUNTRY = Pattern.compile("\\s+/country=\"([^\"]+)\".*");

    public OccurrenceIdIdEnricherGenBank(InteractionListener listener, ImportLogger logger, ResourceService resourceService) {
        super(listener, logger);
        this.resourceService = resourceService;
    }

    public static final Pattern NUCCORE_PREFIX = Pattern.compile("http[s]{0,1}://(www.){0,1}ncbi.nlm.nih.gov/nuccore/([^\\s]+)");


    static void enrichWithGenBankRecord(InputStream is,
                                        String taxonNameField,
                                        String taxonIdField,
                                        String hostTaxonNameField,
                                        String hostBodyPartNameField,
                                        String localeField,
                                        InteractType interactType,
                                        Map<String, String> properties) throws IOException {
        if (is != null) {
            Reader reader = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            boolean inFeatures = false;
            boolean inSource = false;
            while ((line = bufferedReader.readLine()) != null) {
                inFeatures = inFeatures || StringUtils.startsWith(line, "FEATURES");
                inSource = inSource || StringUtils.startsWith(line, "     source");
                if (inFeatures && inSource) {
                    if (!StringUtils.startsWith(line, " ")) {
                        break;
                    }
                    Matcher matcher = PATTERN_ORGANISM.matcher(line);
                    if (matcher.matches()) {
                        properties.putIfAbsent(taxonNameField, matcher.group(1));
                    }

                    matcher = PATTERN_HOST.matcher(line);
                    if (matcher.matches()) {
                        String existingHostName = properties.get(hostTaxonNameField);
                        String foundHostName = matcher.group(1);
                        if (StringUtils.isBlank(existingHostName) || StringUtils.equals(existingHostName, foundHostName)) {
                            properties.putIfAbsent(hostTaxonNameField, foundHostName);
                            properties.putIfAbsent(INTERACTION_TYPE_NAME, interactType.getLabel());
                            properties.putIfAbsent(INTERACTION_TYPE_ID, interactType.getIRI());
                        }
                    }

                    Matcher isolationSource = PATTERN_ISOLATION_SOURCE.matcher(line);
                    if (isolationSource.matches()) {
                        String existingHostBodyPart = properties.get(hostBodyPartNameField);
                        String foundHostBodyPart = isolationSource.group(1);
                        if (StringUtils.isBlank(existingHostBodyPart) || StringUtils.equals(existingHostBodyPart, foundHostBodyPart)) {
                            properties.putIfAbsent(hostBodyPartNameField, foundHostBodyPart);
                        }
                    }

                    matcher = PATTERN_TAXON_ID.matcher(line);
                    if (matcher.matches()) {
                        properties.putIfAbsent(taxonIdField, TaxonomyProvider.ID_PREFIX_NCBI + matcher.group(1));
                    }

                    matcher = PATTERN_COUNTRY.matcher(line);
                    if (matcher.matches()) {
                        properties.putIfAbsent(localeField, matcher.group(1));
                    }
                }
            }
        }
    }

    private InputStream getResponse(String id) throws IOException {
        String prefix = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id=";
        String suffix = "&rettype=gb&retmode=text";
        URI resourceName = URI.create(prefix + id + suffix);
        return resourceService
                .retrieve(resourceName);

    }


    private static String parseNuccoreId(String id) {
        Matcher matcher = NUCCORE_PREFIX.matcher(id);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        throw new IllegalStateException("failed to match [" + id + "]");
    }

    private static boolean isNuccoreId(String id) {
        return StringUtils.isNotBlank(id) && NUCCORE_PREFIX.matcher(id).matches();
    }

    public Map<String, String> enrich(final Map<String, String> properties) throws StudyImporterException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);

        enrichSourceOccurrenceId(enrichedProperties);
        enrichTargetOccurrenceId(enrichedProperties);

        return Collections.unmodifiableMap(enrichedProperties);
    }

    public void enrichTargetOccurrenceId(Map<String, String> enrichedProperties) throws StudyImporterException {
        String occurrenceId = enrichedProperties.get("targetOccurrenceId");
        if (isNuccoreId(occurrenceId)) {
            try (InputStream is = getResponse(parseNuccoreId(occurrenceId))) {
                enrichWithGenBankRecord(is,
                        TARGET_TAXON_NAME,
                        TARGET_TAXON_ID,
                        SOURCE_TAXON_NAME,
                        SOURCE_BODY_PART_NAME,
                        LOCALITY_NAME,
                        InteractType.HOST_OF,
                        enrichedProperties);
            } catch (IOException e) {
                if (logger != null) {
                    logger.warn(LogUtil.contextFor(enrichedProperties), "failed to resolve [" + occurrenceId + "]: " + e.getMessage());
                }
            }

        }
    }

    public void enrichSourceOccurrenceId(Map<String, String> enrichedProperties) throws StudyImporterException {
        String occurrenceId = enrichedProperties.get("sourceOccurrenceId");

        if (isNuccoreId(occurrenceId)) {
            try (InputStream is = getResponse(parseNuccoreId(occurrenceId))) {
                enrichWithGenBankRecord(is,
                        SOURCE_TAXON_NAME,
                        SOURCE_TAXON_ID,
                        TARGET_TAXON_NAME,
                        TARGET_BODY_PART_NAME,
                        LOCALITY_NAME,
                        InteractType.HAS_HOST,
                        enrichedProperties);
            } catch (IOException e) {
                if (logger != null) {
                    logger.warn(LogUtil.contextFor(enrichedProperties), "failed to resolve [" + occurrenceId + "]: " + e.getMessage());
                }
            }
        }
    }


    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        emit(enrich(interaction));
    }
}
