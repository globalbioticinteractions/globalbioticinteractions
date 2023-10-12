package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerClosable;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.globalbioticinteractions.dataset.DwCAUtil;
import org.globalbioticinteractions.util.MapDBUtil;
import org.mapdb.BTreeMap;
import org.mapdb.DB;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eol.globi.data.DatasetImporterForTSV.BASIS_OF_RECORD_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.DatasetImporterForTSV.RESOURCE_TYPES;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_SEX_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_FIELD_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_LIFE_STAGE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_SEX_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_CLASS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_FAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_GENUS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_KINGDOM;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PHYLUM;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_RANK;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBFAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBGENUS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_CLASS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_FAMILY;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_GENUS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_KINGDOM;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PHYLUM;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_RANK;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBFAMILY;

public class DatasetImporterForDwCA extends DatasetImporterWithListener {
    static final String EXTENSION_ASSOCIATED_TAXA = "http://purl.org/NET/aec/associatedTaxa";
    static final String EXTENSION_RESOURCE_RELATIONSHIP = "http://rs.tdwg.org/dwc/terms/ResourceRelationship";
    private static final String EXTENSION_TAXON = "http://rs.tdwg.org/dwc/terms/Taxon";

    // ex. notation used to indicate host of a specimen.
    static final Pattern EX_NOTATION = Pattern.compile("^ex[ .]+(.*)", Pattern.CASE_INSENSITIVE);
    static final Pattern REARED_EX_NOTATION = Pattern.compile("^reared ex[ .]+.*", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_ASSOCIATED_TAXA_IDEA = Pattern.compile("(\\w+)\\W+(\\w+)(:)(.*idae)");
    static final Pattern PATTERN_ASSOCIATED_TAXA_EAE = Pattern.compile("(.*eae):(.*):(.*)");


    private static final String EXTENSION_DESCRIPTION = "http://rs.gbif.org/terms/1.0/Description";
    private static final String EXTENSION_REFERENCE = "http://rs.gbif.org/terms/1.0/Reference";
    private static final String DWC_COREID = "dwc:coreid";

    private static final Pattern ARCTOS_ASSOCIATED_OCCURRENCES_MCZ_DEPS_VERB_PATTERN =
            Pattern.compile("^([(][a-zA-Z ]+[)])[ ](.*)(http[s]{0,1}://mczbase.mcz.harvard.edu/guid/)([a-zA-Z0-9:-]+)");
    private static final Pattern ARCTOS_ASSOCIATED_OCCURRENCES_VERB_PATTERN = Pattern.compile("^[(][a-zA-Z ]+[)][ ]");

    private static final Pattern NEON_ASSOCIATED_OCCURRENCES_PATTERN =
            Pattern.compile("^(?<verb>[a-zA-Z ]+)[:](.*)(http[s]{0,1}://.*)(index.php[?]guid=)(?<occurrenceId>[a-zA-Z0-9:-]+)");

    private static final Pattern MCZ_ASSOCIATED_OCCURRENCES_VERB_PATTERN =
            Pattern.compile("^(.*)" +
                    "<a href=\"(.*)/SpecimenDetail.*collection_object_id=[0-9]+\">[ ]+" +
                    "([^<]+)</a>");

    private static final Map<String, String> PATCHES_FOR_USNM_HOST_OCCURRENCE_REMARKS = new TreeMap<String, String>() {{

        String patchFile = "/org/eol/globi/data/usnm/usnm-patches.tsv";
        try {
            InputStream is = getClass().getResourceAsStream(patchFile);
            String[] lines = StringUtils.split(IOUtils.toString(is, StandardCharsets.UTF_8), "\n");
            for (String line : lines) {
                String[] values = StringUtils.split(line, "\t");
                if (values.length < 2) {
                    throw new IllegalArgumentException("at least two columns expected in patch file [" + patchFile + "]");
                }
                put(values[0], values[1]);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("patch file [" + patchFile + "] is invalid or does not exist", e);
        }

    }};
    private static Pattern INATURALIST_TAXON =
            Pattern.compile("http[s]{0,1}://(www.){0,1}inaturalist.org/taxa/(?<taxonId>[0-9]+)");
    ;


    public DatasetImporterForDwCA(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    public static Map<String, String> parseHitByCarRemarks(String occurrenceRemarks) {
        return new CarRemarksParser().parse(occurrenceRemarks);
    }

    public static Map<String, String> parseKilledByWindow(String occurrenceRemarks) {
        return new WindowRemarksParser().parse(occurrenceRemarks);


    }

    public static Map<String, String> parseKilledByDog(String occurrenceRemarks) {
        final Pattern KILLED_BY_WINDOW
                = Pattern.compile(
                ".*(dog kill).*",
                Pattern.CASE_INSENSITIVE
        );

        Map<String, String> properties = new TreeMap<>();

        if (KILLED_BY_WINDOW.matcher(occurrenceRemarks).matches()) {
            properties.put(TaxonUtil.TARGET_TAXON_NAME, "dog");

            properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
            properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

        }
        return properties;
    }

    public static Map<String, String> parseKilledByCat(String occurrenceRemarks) {
        return new CatRemarksParser().parse(occurrenceRemarks);
    }

    public static Map<String, String> parseEuthanizedRemarks(String occurrenceRemarks) {
        return new EuthanizedRemarksParser(occurrenceRemarks).parse(occurrenceRemarks);
    }

    public static Map<String, String> parseHighVoltageRemarks(String occurrenceRemarks) {
        final Pattern EUTHANIZED_PATTERN
                = Pattern.compile(
                ".*(high voltage|hvt|high voltage trauma).*",
                Pattern.CASE_INSENSITIVE
        );

        Map<String, String> properties = new TreeMap<>();

        if (EUTHANIZED_PATTERN.matcher(occurrenceRemarks).matches()) {
            properties.put(TaxonUtil.TARGET_TAXON_NAME, "high voltage");

            properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
            properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

        }
        return properties;
    }

    public static Map<String, String> parseHitByVehicleRemarks(String occurrenceRemarks) {
        final Pattern HIT_BY_VEHICLE_NOTATION
                = Pattern.compile(
                "(hit by vehicle|hbv|road kill|dead on road).*",
                Pattern.CASE_INSENSITIVE
        );

        Map<String, String> properties = new TreeMap<>();

        if (HIT_BY_VEHICLE_NOTATION.matcher(occurrenceRemarks).matches()) {
            properties.put(TaxonUtil.TARGET_TAXON_NAME, "vehicle");

            properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
            properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

        }
        return properties;
    }


    @Override
    public void importStudy() throws StudyImporterException {
        URI archiveURI = getDataset().getArchiveURI();
        Path tmpDwA = null;
        Thread deleteOnShutdownHook = null;
        try {
            if (getDataset() == null) {
                throw new IllegalArgumentException("no dataset found");
            }

            String archiveURL = getDataset().getOrDefault("url", archiveURI == null ? null : archiveURI.toString());
            getLogger().info(null, "[" + archiveURL + "]: indexing interaction records");

            File dwcaFile = null;
            try {
                URI dwcaURI = URI.create(archiveURL);

                tmpDwA = Files.createTempDirectory("dwca");
                final File tmpDir = tmpDwA.toFile();
                deleteOnShutdownHook = addDeleteOnShutdownHook(tmpDir);
                Archive archive;
                if (CacheUtil.isLocalDir(dwcaURI)) {
                    archive = DwCAUtil.archiveFor(dwcaURI, tmpDwA.toString());
                } else {
                    dwcaFile = File.createTempFile("dwca", "tmp.zip");
                    FileUtils.copyToFile(getDataset().retrieve(dwcaURI), dwcaFile);
                    dwcaFile.deleteOnExit();
                    archive = DwCAUtil.archiveFor(dwcaFile.toURI(), tmpDwA.toString());
                }


                InteractionListenerWithContext listenerWithContext = new InteractionListenerWithContext();

                try (InteractionListenerClosable referencingListener = createReferenceEnricher(archive, listenerWithContext)) {

                    importDescriptionExtension(archive, referencingListener, getLogger());

                    importResourceRelationshipExtension(archive, referencingListener);

                    importAssociatedTaxaExtension(archive, referencingListener);

                    int i = importCore(archive, listenerWithContext, archiveURL);
                    getLogger().info(null, "[" + archiveURL + "]: scanned [" + i + "] record(s)");
                }
            } finally {
                removeDeleteOnShutdownHook(deleteOnShutdownHook);
                if (dwcaFile != null && dwcaFile.exists() && dwcaFile.isFile()) {
                    FileUtils.deleteQuietly(dwcaFile);
                }
            }

        } catch (IOException | IllegalStateException e) {
            // catching IllegalStateException to prevents RuntimeException from stopping all
            // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/409
            throw new StudyImporterException("failed to read archive [" + archiveURI + "]", e);
        } finally {
            if (tmpDwA != null) {
                org.apache.commons.io.FileUtils.deleteQuietly(tmpDwA.toFile());
            }
        }
    }

    public void removeDeleteOnShutdownHook(Thread deleteOnShutdownHook) {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/577
        if (deleteOnShutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(deleteOnShutdownHook);
        }
    }

    private Thread addDeleteOnShutdownHook(final File tmpDir) {
        Thread deleteOnShutdownHook = new Thread(() -> {
            try {
                if (tmpDir != null) {
                    FileUtils.deleteDirectory(tmpDir);
                }
            } catch (IOException ex) {
                //
            }
        });
        Runtime.getRuntime().addShutdownHook(deleteOnShutdownHook);
        return deleteOnShutdownHook;
    }

    private int importCore(Archive archive,
                           InteractionListener interactionListener,
                           String archiveURL) throws StudyImporterException {
        AtomicInteger recordCounter = new AtomicInteger(0);
        ClosableIterator<Record> iterator = archive.getCore().iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                Record rec = iterator.next();
                handleRecord(interactionListener, rec);
                recordCounter.incrementAndGet();
            } catch (IllegalStateException ex) {
                LogUtil.logError(getLogger(), "failed to handle dwc record in [" + archiveURL + "]", ex);
            }
        }
        return recordCounter.get();

    }

    private void handleRecord(InteractionListener interactionListener, Record rec) throws StudyImporterException {
        List<Map<String, String>> interactionCandidates = new ArrayList<>();

        appendInteractionCandidatesIfAvailable(rec, interactionCandidates, DwcTerm.associatedTaxa, "");

        appendInteractionCandidatesIfAvailable(rec, interactionCandidates, DwcTerm.habitat, null);

        appendAssociatedOccurrencesIfAvailable(rec, interactionCandidates);

        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/755#issuecomment-1029509362
        appendAssociatedSequencesIfAvailable(rec, interactionCandidates);

        String occurrenceRemarks = rec.value(DwcTerm.occurrenceRemarks);
        try {
            addCandidatesFromRemarks(interactionCandidates, occurrenceRemarks);
        } catch (IOException e) {
            if (getLogger() != null) {
                Map<String, String> interactionProperties = new HashMap<>();
                mapCoreProperties(rec, interactionProperties);
                getLogger().warn(LogUtil.contextFor(interactionProperties), e.getMessage());
            }
        }

        String dynamicProperties = rec.value(DwcTerm.dynamicProperties);
        if (StringUtils.isNotBlank(dynamicProperties)) {
            Map<String, String> parsedDynamicProperties = parseDynamicPropertiesForInteractionsOnly(dynamicProperties);
            if (!parsedDynamicProperties.isEmpty()) {
                interactionCandidates.add(parsedDynamicProperties);
            }
        }

        Map<String, String> interaction = new HashMap<>(rec.terms().size());
        for (Term term : rec.terms()) {
            interaction.put(term.qualifiedName(), rec.value(term));
        }

        if (interactionCandidates.isEmpty() && isDependency()) {
            // If no candidates are found,
            // add empty candidate to allow interaction listeners to do indexing/enriching
            interactionCandidates.add(new TreeMap<>());
        }

        for (Map<String, String> interactionProperties : interactionCandidates) {
            interactionProperties.putAll(interaction);
            interactionProperties.put(DWC_COREID, rec.id());
            mapIfAvailable(rec, interactionProperties, BASIS_OF_RECORD_NAME, DwcTerm.basisOfRecord);
            mapCoreProperties(rec, interactionProperties);
            appendResourceType(interactionProperties, rec.rowType());
            interactionListener.on(interactionProperties);
        }

    }

    private void appendAssociatedOccurrencesIfAvailable(Record rec, List<Map<String, String>> interactionCandidates) {
        String associatedOccurrences = rec.value(DwcTerm.associatedOccurrences);
        if (StringUtils.isNotBlank(associatedOccurrences)) {
            interactionCandidates.addAll(parseAssociatedOccurrences(associatedOccurrences));
        }
    }

    private void appendAssociatedSequencesIfAvailable(Record rec, List<Map<String, String>> interactionCandidates) {
        String associatedSequences = rec.value(DwcTerm.associatedSequences);
        if (StringUtils.isNotBlank(associatedSequences)) {
            interactionCandidates.addAll(parseAssociatedSequences(associatedSequences));
        }
    }

    private String appendInteractionCandidatesIfAvailable(Record rec, List<Map<String, String>> interactionCandidates, DwcTerm term, String interactionTypeNameDefault) {
        String associatedTaxa = rec.value(term);
        if (StringUtils.isNotBlank(associatedTaxa)) {
            List<Map<String, String>> associatedTaxonProperties = AssociatedTaxaUtil.parseAssociatedTaxa(associatedTaxa, interactionTypeNameDefault);

            for (Map<String, String> associatedTaxonProperty : associatedTaxonProperties) {
                appendResourceType(associatedTaxonProperty, term.qualifiedName());
                interactionCandidates.add(associatedTaxonProperty);
            }
        }
        return associatedTaxa;
    }

    static void addCandidatesFromRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) throws IOException {
        if (StringUtils.isNotBlank(occurrenceRemarks)) {
            addUSNMStyleHostOccurrenceRemarks(interactionCandidates, occurrenceRemarks);
            addRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(interactionCandidates, occurrenceRemarks);
            String[] remarks = StringUtils.split(occurrenceRemarks, ";,.\":\'");
            for (String remark : remarks) {
                addKilledByPetsRemarks(interactionCandidates, remark);
                addKilledByHumansRemarks(interactionCandidates, remark);
            }
        }
    }

    private static void addKilledByHumansRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) {
        Map<String, String> properties = parseKilledByWindow(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = parseHitByCarRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = parseHitByVehicleRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = parseEuthanizedRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = parseHighVoltageRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }


    }

    private static void addKilledByPetsRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) {
        Map<String, String> properties = parseKilledByCat(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = parseKilledByDog(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
        properties = new AttackRemarksParser().parse(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }

    }

    private boolean isDependency() {
        return StringUtils.equalsIgnoreCase(getDataset().getOrDefault(DatasetConstant.IS_DEPENDENCY, "false"), "true");
    }

    private static void addRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) {
        Map<String, String> properties = parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks);
            interactionCandidates.add(properties);
        }
    }

    static Map<String, String> parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(String occurrenceRemarks) {
        Map<String, String> properties = Collections.emptyMap();

        Matcher matcher = Pattern.compile("^(found in)(.*)(pellet).*", Pattern.CASE_INSENSITIVE).matcher(StringUtils.trim(occurrenceRemarks));
        if (matcher.matches()) {
            properties = new TreeMap<String, String>() {{
                put(TARGET_TAXON_NAME, StringUtils.trim(matcher.group(2)));
                put(INTERACTION_TYPE_NAME, "found in");
                put(TARGET_BODY_PART_NAME, "pellet");
                put(TARGET_BODY_PART_ID, "http://purl.obolibrary.org/obo/UBERON_0036018");
            }};
        }
        return properties;
    }

    // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
    private static void addUSNMStyleHostOccurrenceRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) throws IOException {
        Map<String, String> properties = parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
            appendResourceType(properties, DwcTerm.occurrenceRemarks.qualifiedName());
            interactionCandidates.add(properties);
        }
    }

    private static void mapCoreProperties(Record rec, Map<String, String> interactionProperties) {
        mapSourceProperties(rec, interactionProperties);
        mapLocationAndReferenceInfo(rec, interactionProperties);
    }

    private static void mapLocationAndReferenceInfo(Record rec, Map<String, String> interactionProperties) {
        mapIfAvailable(rec, interactionProperties, LOCALITY_NAME, DwcTerm.locality);
        mapIfAvailable(rec, interactionProperties, LOCALITY_ID, DwcTerm.locationID);
        mapIfAvailable(rec, interactionProperties, DECIMAL_LONGITUDE, DwcTerm.decimalLongitude);
        mapIfAvailable(rec, interactionProperties, DECIMAL_LATITUDE, DwcTerm.decimalLatitude);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForMetaTable.EVENT_DATE, DwcTerm.eventDate);
        mapReferenceInfo(rec, interactionProperties);
    }

    private static void mapSourceProperties(Record rec, Map<String, String> interactionProperties) {
        mapIfAvailable(rec, interactionProperties, SOURCE_OCCURRENCE_ID, DwcTerm.occurrenceID);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForTSV.SOURCE_COLLECTION_CODE, DwcTerm.collectionCode);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForTSV.SOURCE_COLLECTION_ID, DwcTerm.collectionID);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, DwcTerm.institutionCode);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, DwcTerm.catalogNumber);
        mapIfAvailable(rec, interactionProperties, DatasetImporterForTSV.SOURCE_RECORD_NUMBER, DwcTerm.recordNumber);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_ID, DwcTerm.taxonConceptID);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_NAME, DwcTerm.scientificName);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_SPECIFIC_EPITHET, DwcTerm.specificEpithet);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_GENUS, DwcTerm.genus);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_SUBGENUS, DwcTerm.subgenus);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_FAMILY, DwcTerm.family);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_ORDER, DwcTerm.order);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_CLASS, DwcTerm.class_);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_PHYLUM, DwcTerm.phylum);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_KINGDOM, DwcTerm.kingdom);
        mapIfAvailable(rec, interactionProperties, SOURCE_LIFE_STAGE_NAME, DwcTerm.lifeStage);
        mapIfAvailable(rec, interactionProperties, SOURCE_SEX_NAME, DwcTerm.sex);
    }

    static void mapReferenceInfo(Record rec, Map<String, String> interactionProperties) {
        String value = StringUtils.trim(rec.value(DcTerm.references));
        if (StringUtils.isBlank(value)) {
            value = StringUtils.trim(rec.value(DwcTerm.occurrenceID));
        }
        if (StringUtils.isNotBlank(value)) {
            appendResourceType(interactionProperties, rec.rowType());
            interactionProperties.put(REFERENCE_CITATION, value);
            interactionProperties.put(REFERENCE_ID, value);
            try {
                URI referenceURI = new URI(value);
                URL url = referenceURI.toURL();
                interactionProperties.put(REFERENCE_URL, url.toString());
            } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
                // opportunistic extraction of url from references to take advantage of practice used in Symbiota)
            }
        }
    }

    private static void mapIfAvailable(Record rec, Map<String, String> interactionProperties, String key, Term term) {
        String value = rec.value(term);
        mapIfAvailable(interactionProperties, key, value);
    }

    private static void mapIfAvailable(Map<String, String> interactionProperties, String key, String value) {
        if ((StringUtils.isNotBlank(value))) {
            interactionProperties.put(key, value);
        }
    }

    static List<Map<String, String>> parseAssociatedOccurrences(String s) {
        List<Map<String, String>> propertyList = new ArrayList<>();

        String[] relationships = StringUtils.split(s, ";");
        for (String relationship : relationships) {
            String relationshipTrimmed = StringUtils.trim(relationship);
            attemptToParseArctosAssocatedOccurrences(propertyList, relationshipTrimmed);
            attemptToParseMCZAssocatedOccurrences(propertyList, relationshipTrimmed);
            attemptToParseNEONAssocatedOccurrences(propertyList, relationshipTrimmed);
        }
        return propertyList;
    }

    static List<Map<String, String>> parseAssociatedSequences(String s) {
        List<Map<String, String>> propertyList = new ArrayList<>();

        String[] relationships = StringUtils.split(s, ";");
        for (String relationship : relationships) {
            String relationshipTrimmed = StringUtils.trim(relationship);
            Matcher matcher = Pattern.compile("^(?<targetOccurrenceId>http[s]{0,1}://www.ncbi.nlm.nih.gov/nuccore/[a-zA-Z0-9]+)").matcher(relationshipTrimmed);
            if (matcher.find()) {
                TreeMap<String, String> e = new TreeMap<String, String>() {
                    {
                        put(TARGET_OCCURRENCE_ID, matcher.group(TARGET_OCCURRENCE_ID));
                    }
                };
                appendResourceType(e, DwcTerm.associatedSequences);
                propertyList.add(e);
            }
        }
        return propertyList;
    }

    private static void attemptToParseArctosAssocatedOccurrences(List<Map<String, String>> propertyList, String relationshipTrimmed) {
        Matcher matcher = ARCTOS_ASSOCIATED_OCCURRENCES_MCZ_DEPS_VERB_PATTERN.matcher(relationshipTrimmed);
        if (matcher.find()) {
            String verb = matcher.group(1);
            String occurrenceId = matcher.group(4);
            if (StringUtils.isNotBlank(occurrenceId)) {
                TreeMap<String, String> e = new TreeMap<String, String>() {
                    {
                        put(TARGET_OCCURRENCE_ID, StringUtils.trim(occurrenceId));
                        put(INTERACTION_TYPE_NAME, StringUtils.trim(verb));
                    }
                };
                appendAssociatedOccurrencesProperties(propertyList, e);
            }
        } else if (ARCTOS_ASSOCIATED_OCCURRENCES_VERB_PATTERN.matcher(relationshipTrimmed).find()) {
            int i1 = StringUtils.indexOf(relationshipTrimmed, ")");
            if (i1 > -1 && i1 < relationshipTrimmed.length()) {
                String relation = StringUtils.substring(relationshipTrimmed, 0, i1 + 1);
                String targetCollectionAndOccurrenceId = StringUtils.trim(StringUtils.substring(relationshipTrimmed, i1 + 1));
                int i = StringUtils.indexOf(targetCollectionAndOccurrenceId, " ");
                if (i > -1) {
                    String occurrenceId = StringUtils.substring(targetCollectionAndOccurrenceId, i);
                    if (StringUtils.isNotBlank(occurrenceId)) {
                        TreeMap<String, String> properties = new TreeMap<>();
                        properties.put(TARGET_OCCURRENCE_ID, StringUtils.trim(occurrenceId));
                        properties.put(INTERACTION_TYPE_NAME, StringUtils.trim(relation));
                        appendAssociatedOccurrencesProperties(propertyList, properties);
                    }
                }
            }
        }
    }

    private static void appendAssociatedOccurrencesProperties(List<Map<String, String>> propertyList, TreeMap<String, String> e) {
        appendResourceType(e, DwcTerm.associatedOccurrences);
        propertyList.add(e);
    }

    private static void attemptToParseMCZAssocatedOccurrences(List<Map<String, String>> propertyList, String relationshipTrimmed) {
        Matcher matcher = MCZ_ASSOCIATED_OCCURRENCES_VERB_PATTERN.matcher(relationshipTrimmed);
        if (matcher.find()) {
            TreeMap<String, String> properties = new TreeMap<>();
            String dwcTriple = StringUtils.replace(StringUtils.trim(matcher.group(3)), " ", ":");
            properties.put(TARGET_OCCURRENCE_ID, dwcTriple);
            properties.put(INTERACTION_TYPE_NAME, StringUtils.trim(matcher.group(1)));
            appendAssociatedOccurrencesProperties(propertyList, properties);
        }
    }

    private static void attemptToParseNEONAssocatedOccurrences(List<Map<String, String>> propertyList, String relationshipTrimmed) {
        Matcher matcher = NEON_ASSOCIATED_OCCURRENCES_PATTERN.matcher(relationshipTrimmed);
        if (matcher.find()) {
            TreeMap<String, String> properties = new TreeMap<>();
            properties.put(TARGET_OCCURRENCE_ID, matcher.group("occurrenceId"));
            properties.put(INTERACTION_TYPE_NAME, StringUtils.trim(matcher.group("verb")));
            appendAssociatedOccurrencesProperties(propertyList, properties);
        }
    }

    static Map<String, String> parseDynamicPropertiesForInteractionsOnly(String s) {
        Map<String, String> properties = new HashMap<>();
        String[] parts = StringUtils.splitByWholeSeparator(s, ";");
        for (String part : parts) {
            String[] propertyValue = StringUtils.split(part, ":=", 2);
            if (propertyValue.length == 2) {
                properties.put(StringUtils.trim(propertyValue[0]), StringUtils.trim(propertyValue[1]));
            }
        }

        mapManterDynamicProperties(properties);

        if (hasInteractionTypeOrName(properties)) {
            appendResourceType(properties, DwcTerm.dynamicProperties.qualifiedName());
        }
        // only consider dynamic properties if interaction types are defined in it.
        return hasInteractionTypeOrName(properties)
                ? properties
                : Collections.emptyMap();
    }

    private static void mapManterDynamicProperties(Map<String, String> properties) {
        // see https://github.com/globalbioticinteractions/unl-nsm/issues/4
        putIfAbsentAndNotBlank(properties, TARGET_TAXON_NAME, properties.get("verbatim host ID"));
        putIfAbsentAndNotBlank(properties, TARGET_BODY_PART_NAME, properties.get("location in host"));
        putIfAbsentAndNotBlank(properties, TARGET_SEX_NAME, properties.get("verbatim host sex"));
        putIfAbsentAndNotBlank(properties, TARGET_LIFE_STAGE_NAME, properties.get("verbatim host age"));

        putIfAbsentAndNotBlank(properties, SOURCE_LIFE_STAGE_NAME, properties.get("age class"));
        putIfAbsentAndNotBlank(properties, SOURCE_SEX_NAME, properties.get("sex"));

        if (StringUtils.isNoneBlank(properties.get("verbatim host ID"))) {
            properties.put(INTERACTION_TYPE_NAME, InteractType.HAS_HOST.getLabel());
            properties.put(INTERACTION_TYPE_ID, InteractType.HAS_HOST.getIRI());
        }
    }

    private static boolean hasInteractionTypeOrName(Map<String, String> properties) {
        return properties.containsKey(INTERACTION_TYPE_ID)
                || properties.containsKey(INTERACTION_TYPE_NAME);
    }

    static void importAssociatedTaxaExtension(Archive archive, InteractionListener interactionListener) {
        ArchiveFile extension = findResourceExtension(archive, EXTENSION_ASSOCIATED_TAXA);
        if (extension != null) {
            final BTreeMap<String, Map<String, String>> associationsMap = MapDBUtil.createBigMap();
            try {
                ArchiveFile core = archive.getCore();
                importTaxaExtension(interactionListener, extension, core, associationsMap);
            } finally {
                if (associationsMap != null) {
                    associationsMap.close();
                }
            }
        }
    }

    private static void importTaxaExtension(InteractionListener interactionListener, ArchiveFile extension, ArchiveFile core, BTreeMap<String, Map<String, String>> associationsMap) {
        for (Record record : extension) {
            Map<String, String> props = new TreeMap<>();
            termsToMap(record, props);
            associationsMap.put(record.id(), props);
        }

        for (Record coreRecord : core) {
            String id = coreRecord.id();
            if (contains(associationsMap, id)) {
                try {
                    Map<String, String> targetProperties = associationsMap.get(id);
                    TreeMap<String, String> interaction = new TreeMap<>();

                    mapAssociationProperties(targetProperties, interaction);

                    mapCoreProperties(coreRecord, interaction);

                    interaction.put(RESOURCE_TYPES,
                            StringUtils.join(Arrays.asList(core.getRowType().qualifiedName(), extension.getRowType().qualifiedName()), CharsetConstant.SEPARATOR));

                    interactionListener.on(interaction);
                } catch (StudyImporterException e) {
                    //
                }
            }
        }
    }

    private static boolean contains(BTreeMap<String, Map<String, String>> associationsMap, String id) {
        return StringUtils.isNoneBlank(id) && associationsMap.containsKey(id);
    }

    private static void importDescriptionExtension(Archive archive,
                                                   InteractionListener interactionListener,
                                                   ImportLogger logger) {
        ArchiveFile extension = findResourceExtension(archive, EXTENSION_DESCRIPTION);
        if (extension != null) {
            final BTreeMap<String, Map<String, String>> associationsMap = MapDBUtil.createBigMap();
            try {
                ArchiveFile core = archive.getCore();
                importDescriptionExtension(interactionListener, logger, extension, core, associationsMap);
            } finally {
                if (associationsMap != null) {
                    associationsMap.close();
                }
            }
        }
    }

    private static void importDescriptionExtension(InteractionListener interactionListener, ImportLogger logger, ArchiveFile extension, ArchiveFile core, BTreeMap<String, Map<String, String>> associationsMap) {
        for (Record record : extension) {
            Map<String, String> props = new TreeMap<>();
            termsToMap(record, props);
            associationsMap.put(record.id(), props);
        }

        for (Record coreRecord : core) {
            String id = coreRecord.id();
            if (contains(associationsMap, id)) {
                try {
                    Map<String, String> targetProperties = associationsMap.get(id);
                    String referenceCitation = targetProperties.get("http://purl.org/dc/terms/source");
                    String descriptionType = targetProperties.get("http://purl.org/dc/terms/type");

                    if (isUnsupportedDescriptionType(descriptionType)) {
                        if (logger != null) {
                            logger.info(null, "ignoring unsupported taxon description of type [" + descriptionType + "]");
                        }
                    } else {
                        String interactionTypeNameDefault = isEcologyDescription(descriptionType) ? null : "";

                        List<Map<String, String>> maps = AssociatedTaxaUtil.parseAssociatedTaxa(
                                targetProperties.get("http://purl.org/dc/terms/description"),
                                interactionTypeNameDefault);


                        for (Map<String, String> map : maps) {
                            TreeMap<String, String> interaction = new TreeMap<>(map);
                            interaction.put(DWC_COREID, id);
                            mapCoreProperties(coreRecord, interaction);
                            if (StringUtils.isNotBlank(referenceCitation)) {
                                interaction.put(REFERENCE_CITATION, referenceCitation);
                                String urlString = ExternalIdUtil.urlForExternalId(referenceCitation);
                                if (ExternalIdUtil.isSupported(urlString)) {
                                    interaction.put(REFERENCE_URL, urlString);
                                }
                            }
                            interactionListener.on(interaction);
                        }
                    }

                } catch (StudyImporterException e) {
                    //
                }
            }
        }
    }

    private static boolean isUnsupportedDescriptionType(String descriptionType) {
        return !isSupportedDescriptionType(descriptionType);
    }

    private static boolean isSupportedDescriptionType(String descriptionType) {
        return isEcologyDescription(descriptionType)
                || isTaxonAssociationDescription(descriptionType)
                || StringUtils.equalsIgnoreCase(descriptionType, "disease")
                || StringUtils.equalsIgnoreCase(descriptionType, "dispersal");
    }

    private static boolean isTaxonAssociationDescription(String descriptionType) {
        return StringUtils.equalsIgnoreCase(descriptionType, "associations");
    }

    private static boolean isEcologyDescription(String descriptionType) {
        return StringUtils.equalsIgnoreCase(descriptionType, "ecology");
    }

    private static InteractionListenerClosable createReferenceEnricher(Archive archive, final InteractionListener interactionListener) {
        return new InteractionListenerClosable() {

            private BTreeMap<String, Map<String, String>> referenceMap = null;

            @Override
            public void close() {
                if (referenceMap != null) {
                    referenceMap.close();
                    referenceMap = null;
                }
            }

            private void initIfNeeded() {
                if (referenceMap == null) {
                    referenceMap = MapDBUtil.createBigMap();

                    ArchiveFile extension = findResourceExtension(archive, EXTENSION_REFERENCE);
                    if (extension != null) {
                        for (Record record : extension) {
                            Map<String, String> props = new TreeMap<>();
                            termsToMap(record, props);
                            props.put(REFERENCE_CITATION, CitationUtil.citationFor(props));
                            appendResourceType(props, extension.getRowType());
                            referenceMap.put(record.id(), props);
                        }
                    }
                }

            }

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                initIfNeeded();

                String s = interaction.get(DWC_COREID);
                Map<String, String> enrichedLink =
                        contains(referenceMap, s)
                                ? new TreeMap<String, String>(interaction) {{
                            putAll(referenceMap.get(s));
                        }}
                                : interaction;
                interactionListener.on(enrichedLink);
            }
        };

    }

    static void importResourceRelationshipExtension(Archive archive, InteractionListener interactionListener) {

        ArchiveFile resourceExtension = findResourceRelationshipExtension(archive);

        if (resourceExtension != null) {

            final BTreeMap<String, Map<String, Map<String, String>>> termTypeIdPropertyMap
                    = MapDBUtil.createBigMap();
            final DB sourceIdDb = MapDBUtil.tmpDB();
            final DB targetIdDb = MapDBUtil.tmpDB();

            try {
                importResourceRelationshipExtension(
                        archive,
                        interactionListener,
                        resourceExtension,
                        termTypeIdPropertyMap,
                        sourceIdDb,
                        targetIdDb);
            } finally {
                sourceIdDb.close();
                targetIdDb.close();
                if (termTypeIdPropertyMap != null) {
                    termTypeIdPropertyMap.close();
                }
            }
        }
    }

    private static void importResourceRelationshipExtension(
            Archive archive,
            InteractionListener interactionListener,
            ArchiveFile resourceExtension,
            BTreeMap<String, Map<String, Map<String, String>>> termTypeIdPropertyMap,
            DB sourceIdDb,
            DB targetIdDb) {
        final Set<String> referencedSourceIds = MapDBUtil.createBigSet(sourceIdDb);
        final Set<String> referencedTargetIds = MapDBUtil.createBigSet(targetIdDb);

        collectRelatedResourceIds(resourceExtension, referencedSourceIds, referencedTargetIds);

        final List<DwcTerm> termTypes = Arrays.asList(
                DwcTerm.occurrenceID,
                DwcTerm.taxonID
        );

        resolveLocalResourceIds(
                archive,
                termTypeIdPropertyMap,
                referencedSourceIds,
                referencedTargetIds,
                termTypes
        );

        importInteractionsFromResourceRelationships(
                interactionListener,
                resourceExtension,
                termTypeIdPropertyMap,
                termTypes
        );
    }

    private static void importInteractionsFromResourceRelationships(InteractionListener interactionListener,
                                                                    ArchiveFile resourceExtension,
                                                                    Map<String, Map<String, Map<String, String>>> termTypeIdPropMap,
                                                                    List<DwcTerm> termTypes) {
        for (Record record : resourceExtension) {
            Map<String, String> props = new TreeMap<>();

            appendResourceType(props, resourceExtension.getRowType());
            String sourceId = record.value(DwcTerm.resourceID);
            String relationship = record.value(DwcTerm.relationshipOfResource);

            Optional<Term> relationshipOfResourceIDTerm = record
                    .terms()
                    .stream()
                    .filter(x -> StringUtils.equals(x.simpleName(), "relationshipOfResourceID"))
                    .findFirst();

            String relationshipTypeIdValue = relationshipOfResourceIDTerm
                    .map(record::value)
                    .orElse(null);
            String targetId = record.value(DwcTerm.relatedResourceID);

            if (StringUtils.isNotBlank(sourceId)) {

                appendVerbatimResourceRelationsValues(record, props);

                String relationshipAccordingTo = record.value(DwcTerm.relationshipAccordingTo);
                if (StringUtils.isNotBlank(relationshipAccordingTo)) {
                    props.putIfAbsent(REFERENCE_CITATION, relationshipAccordingTo);
                }

                putIfAbsentAndNotBlank(props, INTERACTION_TYPE_NAME, relationship);
                putIfAbsentAndNotBlank(props, INTERACTION_TYPE_ID, relationshipTypeIdValue);
                putIfAbsentAndNotBlank(props, DatasetImporterForMetaTable.EVENT_DATE, record.value(DwcTerm.relationshipEstablishedDate));

                for (DwcTerm termType : termTypes) {
                    String key = termType.qualifiedName();
                    if (StringUtils.isNoneBlank(key) && termTypeIdPropMap.containsKey(key)) {
                        Map<String, Map<String, String>> propMap = termTypeIdPropMap.get(termType.qualifiedName());

                        populatePropertiesAssociatedWithId(
                                props,
                                sourceId,
                                true,
                                propMap.get(sourceId),
                                labelPairFor(termType));

                        extractNameFromRelationshipRemarks(record)
                                .ifPresent(name -> props.put(TARGET_TAXON_NAME, name));

                        populatePropertiesAssociatedWithId(
                                props,
                                targetId,
                                false,
                                propMap.get(targetId),
                                labelPairFor(termType));

                    }

                }

                try {
                    patchINaturalistProperties(props);
                    interactionListener.on(props);
                } catch (StudyImporterException e) {
                    //
                }
            }
        }
    }

    private static void patchINaturalistProperties(Map<String, String> props) {
        String candidateTargetTaxonId = props.get(DwcTerm.relatedResourceID.qualifiedName());
        if (StringUtils.isNotBlank(candidateTargetTaxonId)) {
            Matcher matcher = INATURALIST_TAXON.matcher(candidateTargetTaxonId);
            if (matcher.matches()) {
                String str = props.get(SOURCE_TAXON_ID);
                if (NumberUtils.isDigits(str)) {
                    props.put(SOURCE_TAXON_ID, "https://www.inaturalist.org/taxa/" + str);
                }
                props.put(TARGET_TAXON_ID, candidateTargetTaxonId);
                String citation = props.get(REFERENCE_CITATION);
                if (StringUtils.startsWith(citation, "https://www.inaturalist.org/people")) {
                    String sourceCatalogNumber = props.get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER);
                    if (NumberUtils.isDigits(sourceCatalogNumber)) {
                        String reference = "https://www.inaturalist.org/observations/" + sourceCatalogNumber;
                        props.put(REFERENCE_URL, reference);
                        props.put(REFERENCE_CITATION, reference);
                    }
                }
            }
        }

    }

    private static void appendResourceType(Map<String, String> props, Term rowType) {
        if (rowType != null) {
            String qualifiedName = rowType.qualifiedName();
            appendResourceType(props, qualifiedName);
        }
    }

    private static void appendResourceType(Map<String, String> props, String qualifiedName) {
        if (StringUtils.isNotBlank(qualifiedName)) {
            String prefix = StringUtils.isBlank(props.get(RESOURCE_TYPES))
                    ? ""
                    : props.get(RESOURCE_TYPES) + CharsetConstant.SEPARATOR;

            if (!StringUtils.contains(prefix, qualifiedName)) {
                props.put(RESOURCE_TYPES, prefix + qualifiedName);
            }
        }
    }

    private static Pair<String, String> labelPairFor(DwcTerm termType) {
        Pair<String, String> idLabelPair = Pair.of(
                SOURCE_OCCURRENCE_ID,
                TARGET_OCCURRENCE_ID);

        if (termType.equals(DwcTerm.taxonID)) {
            idLabelPair = Pair.of(SOURCE_TAXON_ID, TARGET_TAXON_ID);
        }
        return idLabelPair;
    }

    private static Optional<String> extractNameFromRelationshipRemarks(Record record) {
        String[] remarks = StringUtils.split(record.value(DwcTerm.relationshipRemarks), CharsetConstant.SEPARATOR_CHAR);

        return remarks == null
                ? Optional.empty()
                : Arrays
                .stream(remarks)
                .map(StringUtils::trim)
                .filter(x -> StringUtils.startsWith(x, "scientificName:"))
                .findFirst()
                .map(x -> RegExUtils.replacePattern(x, "^scientificName[ ]*:[ ]*", ""));
    }

    private static void resolveLocalResourceIds(Archive archive,
                                                Map<String, Map<String, Map<String, String>>> termIdPropMap,
                                                Set<String> referencedSourceIds,
                                                Set<String> referencedTargetIds,
                                                List<DwcTerm> termTypes) {
        List<ArchiveFile> archiveFiles = new ArrayList<>();
        archiveFiles.add(archive.getCore());

        ArchiveFile taxon = findResourceExtension(archive, EXTENSION_TAXON);
        if (taxon != null) {
            archiveFiles.add(taxon);
        }

        for (ArchiveFile archiveFile : archiveFiles) {
            for (Record record : archiveFile) {
                for (DwcTerm termType : termTypes) {
                    attemptLinkUsingTerm(termIdPropMap,
                            referencedSourceIds,
                            referencedTargetIds,
                            record,
                            termType);
                }
            }
        }
    }

    private static void collectRelatedResourceIds(ArchiveFile resourceExtension, Set<String> referencedSourceIds, Set<String> referencedTargetIds) {
        for (Record record : resourceExtension) {
            String targetId = record.value(DwcTerm.relatedResourceID);
            String sourceId = record.value(DwcTerm.resourceID);
            String relationshipRemarks = record.value(DwcTerm.relationshipRemarks);
            if (StringUtils.isNotBlank(sourceId)) {
                if (StringUtils.isNotBlank(targetId)) {
                    referencedSourceIds.add(sourceId);
                    referencedTargetIds.add(targetId);
                } else if (StringUtils.contains(relationshipRemarks, "scientificName:")) {
                    referencedSourceIds.add(sourceId);
                }

            }
        }
    }

    private static void appendVerbatimResourceRelationsValues(Record record, Map<String, String> props) {
        Set<Term> terms = record.terms();
        for (Term term : terms) {
            props.putIfAbsent(term.qualifiedName(), record.value(term));
        }
    }

    private static void attemptLinkUsingTerm(Map<String, Map<String, Map<String, String>>> termIdPropertyMap,
                                             Set<String> referencedSourceIds,
                                             Set<String> referencedTargetIds,
                                             Record coreRecord,
                                             DwcTerm term) {
        String id = coreRecord.value(term);
        if (StringUtils.isNotBlank(id)) {
            Set<String> idCandidates = new TreeSet<>();
            idCandidates.add(id);
            idCandidates.add(RegExUtils.replacePattern(id, "^http://", "https://"));
            if (DwcTerm.taxonID.equals(term)) {
                idCandidates.add(StringUtils.prependIfMissing(id, "https://www.inaturalist.org/taxa/"));
            }

            for (String idCandidate : idCandidates) {
                if (isReferenced(referencedSourceIds, referencedTargetIds, idCandidate)) {
                    linkTerm(termIdPropertyMap, coreRecord, term, idCandidate);
                    break;
                }

            }
        }


    }

    private static void linkTerm(Map<String, Map<String, Map<String, String>>> termIdPropertyMap, Record coreRecord, DwcTerm term, String id) {
        TreeMap<String, String> occProps = new TreeMap<>();
        termsToMap(coreRecord, occProps);
        appendResourceType(occProps, coreRecord.rowType());
        Map<String, Map<String, String>> propMap = termIdPropertyMap.get(term.qualifiedName());
        if (propMap == null) {
            propMap = new HashMap<>();
            propMap.put(id, occProps);
            termIdPropertyMap.put(term.qualifiedName(), propMap);
        } else {
            propMap.put(id, occProps);
        }
    }

    private static boolean isReferenced(Set<String> referencedSourceIds, Set<String> referencedTargetIds, String id) {
        return StringUtils.isNotBlank(id) &&
                (referencedTargetIds.contains(id) || referencedSourceIds.contains(id));
    }

    private static void populatePropertiesAssociatedWithId(Map<String, String> props,
                                                           String id,
                                                           boolean isSource,
                                                           Map<String, String> occurrenceProperties,
                                                           Pair<String, String> idLabelPairs) {
        putIfAbsentAndNotBlank(props, isSource ? idLabelPairs.getLeft() : idLabelPairs.getRight(), id);

        if (occurrenceProperties != null) {
            putIfAbsentAndNotBlank(props, isSource ? DatasetImporterForTSV.SOURCE_INSTITUTION_CODE : DatasetImporterForTSV.TARGET_INSTITUTION_CODE, occurrenceProperties.get(DwcTerm.institutionCode.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? DatasetImporterForTSV.SOURCE_COLLECTION_ID : DatasetImporterForTSV.TARGET_COLLECTION_ID, occurrenceProperties.get(DwcTerm.collectionID.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? DatasetImporterForTSV.SOURCE_COLLECTION_CODE : DatasetImporterForTSV.TARGET_COLLECTION_CODE, occurrenceProperties.get(DwcTerm.collectionCode.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? DatasetImporterForTSV.SOURCE_CATALOG_NUMBER : DatasetImporterForTSV.TARGET_CATALOG_NUMBER, occurrenceProperties.get(DwcTerm.catalogNumber.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_ID : TARGET_TAXON_ID, occurrenceProperties.get(DwcTerm.taxonID.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_NAME : TARGET_TAXON_NAME, occurrenceProperties.get(DwcTerm.scientificName.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_RANK : TARGET_TAXON_RANK, occurrenceProperties.get(DwcTerm.taxonRank.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_SPECIFIC_EPITHET : TARGET_TAXON_SPECIFIC_EPITHET, occurrenceProperties.get(DwcTerm.specificEpithet.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_GENUS : TARGET_TAXON_GENUS, occurrenceProperties.get(DwcTerm.genus.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_FAMILY : TARGET_TAXON_FAMILY, occurrenceProperties.get(DwcTerm.family.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_SUBFAMILY : TARGET_TAXON_SUBFAMILY, occurrenceProperties.get(DwcTerm.subfamily.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_ORDER : TARGET_TAXON_ORDER, occurrenceProperties.get(DwcTerm.order.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_CLASS : TARGET_TAXON_CLASS, occurrenceProperties.get(DwcTerm.class_.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_PHYLUM : TARGET_TAXON_PHYLUM, occurrenceProperties.get(DwcTerm.phylum.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_KINGDOM : TARGET_TAXON_KINGDOM, occurrenceProperties.get(DwcTerm.kingdom.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_SEX_NAME : TARGET_SEX_NAME, occurrenceProperties.get(DwcTerm.sex.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_LIFE_STAGE_NAME : TARGET_LIFE_STAGE_NAME, occurrenceProperties.get(DwcTerm.lifeStage.qualifiedName()));
            putIfAbsentAndNotBlank(props, LOCALITY_NAME, occurrenceProperties.get(DwcTerm.locality.qualifiedName()));
            putIfAbsentAndNotBlank(props, DECIMAL_LATITUDE, occurrenceProperties.get(DwcTerm.decimalLatitude.qualifiedName()));
            putIfAbsentAndNotBlank(props, DECIMAL_LONGITUDE, occurrenceProperties.get(DwcTerm.decimalLongitude.qualifiedName()));
            putIfAbsentAndNotBlank(props, DatasetImporterForMetaTable.EVENT_DATE, occurrenceProperties.get(DwcTerm.eventDate.qualifiedName()));
            putIfAbsentAndNotBlank(props, BASIS_OF_RECORD_NAME, occurrenceProperties.get(DwcTerm.basisOfRecord.qualifiedName()));
            appendResourceType(props, occurrenceProperties.get(RESOURCE_TYPES));
        }
    }

    private static void putIfAbsentAndNotBlank(Map<String, String> props, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            props.putIfAbsent(key, value);
        }
    }

    private static ArchiveFile findResourceRelationshipExtension(Archive archive) {
        return findResourceExtension(archive, EXTENSION_RESOURCE_RELATIONSHIP);
    }

    static ArchiveFile findResourceExtension(Archive archive, String extensionType) {
        ArchiveFile resourceRelationExtension = null;
        Set<ArchiveFile> extensions = archive.getExtensions();
        for (ArchiveFile extension : extensions) {
            if (StringUtils.equals(extension.getRowType().qualifiedName(),
                    extensionType)) {
                resourceRelationExtension = extension;
                break;
            }
        }
        return resourceRelationExtension;
    }

    private static void termsToMap(Record record, Map<String, String> props) {
        for (Term term : record.terms()) {
            String value = record.value(term);
            if (StringUtils.isNotBlank(value)) {
                props.put(term.qualifiedName(), value);
            }
            appendResourceType(props, record.rowType());
        }
    }

    private static void mapAssociationProperties(Map<String, String> targetProperties, TreeMap<String, String> interaction) {
        DatasetImporterForDwCA.mapIfAvailable(
                interaction,
                DatasetImporterForTSV.BASIS_OF_RECORD_NAME,
                targetProperties.get("http://rs.tdwg.org/dwc/terms/basisOfRecord")
        );

        String scientificName = targetProperties.get("http://purl.org/NET/aec/associatedScientificName");
        String specificEpithet = targetProperties.get("http://purl.org/NET/aec/associatedSpecificEpithet");
        String genus = targetProperties.get("http://purl.org/NET/aec/associatedGenus");

        String targetName = (StringUtils.isNotBlank(genus) && StringUtils.isNotBlank(specificEpithet))
                ? StringUtils.join(new String[]{genus, specificEpithet}, " ")
                : scientificName;

        DatasetImporterForDwCA.mapIfAvailable(
                interaction,
                TaxonUtil.TARGET_TAXON_NAME,
                targetName
        );

        DatasetImporterForDwCA.mapIfAvailable(
                interaction,
                TaxonUtil.TARGET_TAXON_NAME,
                targetProperties.get("http://purl.org/NET/aec/associatedScientificName")
        );

        DatasetImporterForDwCA.mapIfAvailable(
                interaction,
                DatasetImporterForTSV.INTERACTION_TYPE_ID,
                targetProperties.get("http://purl.org/NET/aec/associatedRelationshipURI")
        );

        DatasetImporterForDwCA.mapIfAvailable(
                interaction,
                DatasetImporterForTSV.INTERACTION_TYPE_NAME,
                targetProperties.get("http://purl.org/NET/aec/associatedRelationshipTerm")
        );

        appendResourceType(interaction, targetProperties.get(RESOURCE_TYPES));


    }

    private static class CarRemarksParser implements RemarksParser {
        @Override
        public Map<String, String> parse(String occurrenceRemarks) {
            final Pattern HIT_BY_CAR_NOTATION
                    = Pattern.compile(
                    ".*(hit by car).*",
                    Pattern.CASE_INSENSITIVE
            );

            Map<String, String> properties = new TreeMap<>();

            if (HIT_BY_CAR_NOTATION.matcher(occurrenceRemarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "car");
                properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
                properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());
            }
            return properties;
        }
    }

    private static class WindowRemarksParser implements RemarksParser {
        @Override
        public Map<String, String> parse(String remarks) {
            final Pattern KILLED_BY_WINDOW
                    = Pattern.compile(
                    ".*(window strike|window kill).*",
                    Pattern.CASE_INSENSITIVE
            );

            Map<String, String> properties = new TreeMap<>();

            if (KILLED_BY_WINDOW.matcher(remarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "window");

                properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
                properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

            }
            return properties;

        }
    }

    private static class CatRemarksParser implements RemarksParser {

        @Override
        public Map<String, String> parse(String remarks) {
            final Pattern KILLED_BY_CAT
                    = Pattern.compile(
                    ".*(cat kill).*",
                    Pattern.CASE_INSENSITIVE
            );

            Map<String, String> properties = new TreeMap<>();

            if (KILLED_BY_CAT.matcher(remarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "cat");

                properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
                properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

            }
            return properties;

        }
    }

    private static class EuthanizedRemarksParser implements RemarksParser {
        private final String occurrenceRemarks;

        public EuthanizedRemarksParser(String occurrenceRemarks) {
            this.occurrenceRemarks = occurrenceRemarks;
        }

        @Override
        public Map<String, String> parse(String remarks) {
            final Pattern EUTHANIZED_PATTERN
                    = Pattern.compile(
                    ".*(euthanized).*",
                    Pattern.CASE_INSENSITIVE
            );

            Map<String, String> properties = new TreeMap<>();

            if (EUTHANIZED_PATTERN.matcher(occurrenceRemarks).matches()) {
                properties.put(TaxonUtil.TARGET_TAXON_NAME, "euthanasia");

                properties.put(INTERACTION_TYPE_NAME, InteractType.KILLED_BY.getLabel());
                properties.put(INTERACTION_TYPE_ID, InteractType.KILLED_BY.getIRI());

            }
            return properties;
        }
    }

    private class InteractionListenerWithContext implements InteractionListener {

        @Override
        public void on(Map<String, String> interaction) throws StudyImporterException {
            if (getDataset() == null) {
                getInteractionListener().on(interaction);
            } else {
                getInteractionListener().on(new TreeMap<String, String>(interaction) {{
                    if (getDataset().getArchiveURI() != null) {
                        put(DatasetConstant.ARCHIVE_URI, getDataset().getArchiveURI().toString());
                    }
                    put(DatasetConstant.CONTENT_HASH, getDataset().getOrDefault(DatasetConstant.CONTENT_HASH, ""));

                    String sourceCitationTrimmed = CitationUtil.sourceCitationLastAccessed(getDataset());
                    DatasetImporterForDwCA.putIfAbsentAndNotBlank(this, DATASET_CITATION, sourceCitationTrimmed);
                    DatasetImporterForDwCA.putIfAbsentAndNotBlank(this, REFERENCE_CITATION, sourceCitationTrimmed);
                    DatasetImporterForDwCA.putIfAbsentAndNotBlank(this, REFERENCE_ID, sourceCitationTrimmed);

                }});
            }

        }

    }

    public static Map<String, String> parseUSNMStyleHostOccurrenceRemarks(String occurrenceRemarks) throws IOException {
        Map<String, String> properties = Collections.emptyMap();
        String candidateJsonChunk = null;
        String[] split = StringUtils.splitPreserveAllTokens(occurrenceRemarks, "{");
        if (split.length > 1) {
            String[] splitClosing = StringUtils.splitPreserveAllTokens(split[1], "}");
            if (splitClosing.length > 1) {
                candidateJsonChunk = "{" + splitClosing[0] + "}";
            }
        }

        if (StringUtils.isNotBlank(candidateJsonChunk)) {
            try {

                properties = parseJsonChunk(
                        attemptToPatchOccurrenceRemarksWithMalformedJSON(candidateJsonChunk)
                );
            } catch (IOException ex) {
                if (StringUtils.contains(candidateJsonChunk, "hostGen")) {
                    throw new IOException("found likely malformed host description [" + candidateJsonChunk + "], see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/505");
                }
            }
        }
        return properties;
    }

    public static Map<String, String> parseJsonChunk(String candidateJsonChunk) throws IOException {
        Map<String, String> properties = new TreeMap<>();
        JsonNode jsonNode = new ObjectMapper().readTree(candidateJsonChunk);

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isValueNode()) {
                properties.put(field.getKey(), field.getValue().asText());
            }
        }

        setPropertyIfExists(properties, jsonNode, "hostGen", TaxonUtil.TARGET_TAXON_GENUS);
        setPropertyIfExists(properties, jsonNode, "hostSpec", TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET);
        setPropertyIfExists(properties, jsonNode, "hostBodyLoc", TARGET_BODY_PART_NAME);
        setPropertyIfExists(properties, jsonNode, "hostFldNo", TARGET_FIELD_NUMBER);
        setPropertyIfExists(properties, jsonNode, "hostMusNo", TARGET_CATALOG_NUMBER);

        String value = jsonNode.has("hostHiTax") ? jsonNode.get("hostHiTax").asText() : null;
        if (StringUtils.isNotBlank(value)) {
            String[] taxa = StringUtils.split(value, " :");
            List<String> path = new ArrayList<>(Arrays.asList(taxa));
            List<String> pathNames = new ArrayList<>(Collections.nCopies(path.size() - 1, "|"));
            if (properties.containsKey(TARGET_TAXON_GENUS)) {
                path.add(properties.get(TARGET_TAXON_GENUS));
                pathNames.add("| genus");
            }
            if (properties.containsKey(TARGET_TAXON_SPECIFIC_EPITHET) && properties.containsKey(TARGET_TAXON_GENUS)) {
                String speciesName = TaxonUtil.generateTargetTaxonName(properties);
                properties.put(TARGET_TAXON_NAME, speciesName);
                path.add(speciesName);
                pathNames.add("| species");
            }
            properties.put(TARGET_TAXON_PATH, StringUtils.join(path, CharsetConstant.SEPARATOR));
            properties.put(TARGET_TAXON_PATH_NAMES, StringUtils.join(pathNames, " "));
        }

        if (MapUtils.isNotEmpty(properties)) {
            properties.put(INTERACTION_TYPE_NAME, InteractType.HAS_HOST.getLabel());
            properties.put(INTERACTION_TYPE_ID, InteractType.HAS_HOST.getIRI());
            properties = TaxonUtil.enrichTaxonNames(properties);
        }
        return properties;
    }

    private static void setPropertyIfExists(Map<String, String> properties, JsonNode jsonNode, String remarkKey, String propertyName) {
        String value = jsonNode.has(remarkKey) ? jsonNode.get(remarkKey).asText() : null;
        if (StringUtils.isNotBlank(value)) {
            properties.put(propertyName, StringUtils.trim(value));
        }
    }

    static String attemptToPatchOccurrenceRemarksWithMalformedJSON(String occurrenceRemarks) {
        // see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/504
        for (Map.Entry<String, String> replacement : PATCHES_FOR_USNM_HOST_OCCURRENCE_REMARKS.entrySet()) {
            occurrenceRemarks = occurrenceRemarks.replace(replacement.getKey(), replacement.getValue());
        }
        return occurrenceRemarks;
    }


}
