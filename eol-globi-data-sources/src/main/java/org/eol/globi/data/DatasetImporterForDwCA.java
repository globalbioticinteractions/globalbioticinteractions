package org.eol.globi.data;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.InteractTypeMapper;
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
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
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
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET;

public class DatasetImporterForDwCA extends DatasetImporterWithListener {
    public static final String EXTENSION_ASSOCIATED_TAXA = "http://purl.org/NET/aec/associatedTaxa";
    public static final String EXTENSION_RESOURCE_RELATIONSHIP = "http://rs.tdwg.org/dwc/terms/ResourceRelationship";
    public static final String EXTENSION_TAXON = "http://rs.tdwg.org/dwc/terms/Taxon";

    // ex. notation used to indicate host of a specimen.
    public static final Pattern EX_NOTATION = Pattern.compile("^ex.+\\W.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern REARED_EX_NOTATION = Pattern.compile("^reared ex.+\\W.*", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_ASSOCIATED_TAXA_IDEA = Pattern.compile("(\\w+)\\W+(\\w+)(:)(.*idae)");
    public static final Pattern PATTERN_ASSOCIATED_TAXA_EAE = Pattern.compile("(.*eae):(.*):(.*)");
    public static final String EXTENSION_DESCRIPTION = "http://rs.gbif.org/terms/1.0/Description";
    public static final String EXTENSION_REFERENCE = "http://rs.gbif.org/terms/1.0/Reference";
    public static final String DWC_COREID = "dwc:coreid";


    public DatasetImporterForDwCA(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
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

                InteractionListener referencingListener = createReferenceEnricher(archive, listenerWithContext);

                importDescriptionExtension(archive, referencingListener);

                importResourceRelationExtension(archive, referencingListener);

                importAssociatedTaxaExtension(archive, referencingListener);

                int i = importCore(archive, listenerWithContext);
                getLogger().info(null, "[" + archiveURL + "]: scanned [" + i + "] record(s)");
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

    private int importCore(Archive archive, InteractionListener interactionListener) throws StudyImporterException {
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
                LogUtil.logError(getLogger(), "failed to handle dwc record", ex);
            }
        }
        return recordCounter.get();

    }

    private void handleRecord(InteractionListener interactionListener, Record rec) throws StudyImporterException {
        List<Map<String, String>> interactionCandidates = new ArrayList<>();


        String associatedTaxa = rec.value(DwcTerm.associatedTaxa);
        if (StringUtils.isNotBlank(associatedTaxa)) {
            interactionCandidates.addAll(AssociatedTaxaUtil.parseAssociatedTaxa(associatedTaxa));
        }

        String associatedOccurrences = rec.value(DwcTerm.associatedOccurrences);
        if (StringUtils.isNotBlank(associatedOccurrences)) {
            interactionCandidates.addAll(parseAssociatedOccurrences(associatedOccurrences));
        }

        String occurrenceRemarks = rec.value(DwcTerm.occurrenceRemarks);
        if (StringUtils.isNotBlank(occurrenceRemarks)) {
            try {
                addUSNMStyleHostOccurrenceRemarks(interactionCandidates, occurrenceRemarks);
                addRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(interactionCandidates, occurrenceRemarks);
            } catch (IOException e) {
                if (getLogger() != null) {
                    Map<String, String> interactionProperties = new HashMap<>();
                    mapCoreProperties(rec, interactionProperties);
                    getLogger().warn(LogUtil.contextFor(interactionProperties), e.getMessage());
                }
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

        for (Map<String, String> interactionProperties : interactionCandidates) {
            interactionProperties.putAll(interaction);
            interactionProperties.put(DWC_COREID, rec.id());
            mapIfAvailable(rec, interactionProperties, BASIS_OF_RECORD_NAME, DwcTerm.basisOfRecord);
            mapCoreProperties(rec, interactionProperties);
            interactionListener.on(interactionProperties);
        }
    }

    private static void addRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) {
        Map<String, String> properties = parseRoyalSaskatchewanMuseumOwlPelletCollectionStyleRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
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
    private void addUSNMStyleHostOccurrenceRemarks(List<Map<String, String>> interactionCandidates, String occurrenceRemarks) throws IOException {
        Map<String, String> properties = parseUSNMStyleHostOccurrenceRemarks(occurrenceRemarks);
        if (MapUtils.isNotEmpty(properties)) {
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
                        propertyList.add(properties);
                    }
                }
            }

        }
        return propertyList;
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
        // only consider dynamic properties if interaction types are defined in it.
        return properties.containsKey(INTERACTION_TYPE_ID)
                || properties.containsKey(INTERACTION_TYPE_NAME)
                ? properties
                : Collections.emptyMap();
    }

    static void importAssociatedTaxaExtension(Archive archive, InteractionListener interactionListener) {
        ArchiveFile extension = findResourceExtension(archive, EXTENSION_ASSOCIATED_TAXA);
        if (extension != null) {
            ArchiveFile core = archive.getCore();

            DB db = DBMaker
                    .newMemoryDirectDB()
                    .compressionEnable()
                    .transactionDisable()
                    .make();
            final HTreeMap<String, Map<String, String>> associationsMap = db
                    .createHashMap("assocMap")
                    .make();

            for (Record record : extension) {
                Map<String, String> props = new TreeMap<>();
                termsToMap(record, props);
                associationsMap.put(record.id(), props);
            }

            for (Record coreRecord : core) {
                String id = coreRecord.id();
                if (associationsMap.containsKey(id)) {
                    try {
                        Map<String, String> targetProperties = associationsMap.get(id);
                        TreeMap<String, String> interaction = new TreeMap<>();

                        mapAssociationProperties(targetProperties, interaction);

                        mapCoreProperties(coreRecord, interaction);

                        interactionListener.on(interaction);
                    } catch (StudyImporterException e) {
                        //
                    }
                }
            }
        }
    }

    private static void importDescriptionExtension(Archive archive, InteractionListener interactionListener) {
        ArchiveFile extension = findResourceExtension(archive, EXTENSION_DESCRIPTION);
        if (extension != null) {
            ArchiveFile core = archive.getCore();

            DB db = DBMaker
                    .newMemoryDirectDB()
                    .compressionEnable()
                    .transactionDisable()
                    .make();
            final HTreeMap<String, Map<String, String>> associationsMap = db
                    .createHashMap("assocMap")
                    .make();

            for (Record record : extension) {
                Map<String, String> props = new TreeMap<>();
                termsToMap(record, props);
                associationsMap.put(record.id(), props);
            }

            for (Record coreRecord : core) {
                String id = coreRecord.id();
                if (associationsMap.containsKey(id)) {
                    try {
                        Map<String, String> targetProperties = associationsMap.get(id);
                        List<Map<String, String>> maps = AssociatedTaxaUtil.parseAssociatedTaxa(targetProperties.get("http://purl.org/dc/terms/description"));
                        for (Map<String, String> map : maps) {
                            TreeMap<String, String> interaction = new TreeMap<>(map);
                            interaction.put(DWC_COREID, id);
                            mapCoreProperties(coreRecord, interaction);
                            interactionListener.on(interaction);
                        }

                    } catch (StudyImporterException e) {
                        //
                    }
                }
            }
        }
    }

    private static InteractionListener createReferenceEnricher(Archive archive, final InteractionListener interactionListener) {
        return new InteractionListener() {
            private Map<String, Map<String, String>> referenceMap = null;

            private void initIfNeeded() {
                if (referenceMap == null) {
                    referenceMap = Collections.emptyMap();

                    ArchiveFile extension = findResourceExtension(archive, EXTENSION_REFERENCE);
                    if (extension != null) {
                        DB db = DBMaker
                                .newMemoryDirectDB()
                                .compressionEnable()
                                .transactionDisable()
                                .make();
                        referenceMap = db
                                .createHashMap("referenceMap")
                                .make();

                        for (Record record : extension) {
                            Map<String, String> props = new TreeMap<>();
                            termsToMap(record, props);
                            props.put(REFERENCE_CITATION, CitationUtil.citationFor(props));
                            referenceMap.put(record.id(), props);
                        }
                    }
                }

            }

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                initIfNeeded();

                String s = interaction.get(DWC_COREID);
                Map<String, String> enrichedLink = referenceMap.containsKey(s)
                        ? new TreeMap(interaction) {{
                    putAll(referenceMap.get(s));
                }}
                        : interaction;
                interactionListener.on(enrichedLink);
            }
        };

    }

    static void importResourceRelationExtension(Archive archive, InteractionListener interactionListener) {

        ArchiveFile resourceExtension = findResourceRelationshipExtension(archive);

        if (resourceExtension != null) {

            DB db = DBMaker
                    .newMemoryDirectDB()
                    .compressionEnable()
                    .transactionDisable()
                    .make();

            final Map<String, Map<String, Map<String, String>>> termTypeIdPropertyMap = db
                    .createHashMap("termIdPropMap")
                    .make();

            final Set<String> referencedSourceIds = db
                    .createHashSet("sourceIdMap")
                    .make();

            final Set<String> referencedTargetIds = db
                    .createHashSet("targetIdMap")
                    .make();


            collectRelatedResourceIds(resourceExtension, referencedSourceIds, referencedTargetIds);

            final List<DwcTerm> termTypes = Arrays.asList(
                    DwcTerm.occurrenceID, DwcTerm.taxonID);

            resolveLocalResourceIds(archive, termTypeIdPropertyMap, referencedSourceIds, referencedTargetIds, termTypes);

            importInteractionsFromResourceRelationships(interactionListener, resourceExtension, termTypeIdPropertyMap, termTypes);
        }
    }

    private static void importInteractionsFromResourceRelationships(InteractionListener interactionListener,
                                                                    ArchiveFile resourceExtension,
                                                                    Map<String, Map<String, Map<String, String>>> termTypeIdPropMap,
                                                                    List<DwcTerm> termTypes) {
        for (Record record : resourceExtension) {
            Map<String, String> props = new TreeMap<>();
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
                    if (termTypeIdPropMap.containsKey(termType.qualifiedName())) {
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
                    interactionListener.on(props);
                } catch (StudyImporterException e) {
                    //
                }
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
                .map(x -> StringUtils.replacePattern(x, "^scientificName[ ]*:[ ]*", ""));
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

    private static String findRelationshipTypeIdByLabel(String relationship, InteractTypeMapper mapper) {

        String relationshipKey = StringUtils.lowerCase(StringUtils.trim(relationship));

        String relationshipId = null;
        if (StringUtils.isNotBlank(relationship)) {
            InteractType interactType = mapper.getInteractType(relationshipKey);
            relationshipId = interactType == null ? null : interactType.getIRI();
        }

        return relationshipId;
    }

    private static void attemptLinkUsingTerm(Map<String, Map<String, Map<String, String>>> termIdPropertyMap,
                                             Set<String> referencedSourceIds,
                                             Set<String> referencedTargetIds,
                                             Record coreRecord,
                                             DwcTerm term) {
        String id = coreRecord.value(term);
        if (StringUtils.isNotBlank(id) &&
                (referencedTargetIds.contains(id) || referencedSourceIds.contains(id))) {
            TreeMap<String, String> occProps = new TreeMap<>();
            termsToMap(coreRecord, occProps);

            Map<String, Map<String, String>> propMap = termIdPropertyMap.get(term.qualifiedName());
            if (propMap == null) {
                propMap = new HashMap<>();
                propMap.put(id, occProps);
                termIdPropertyMap.put(term.qualifiedName(), propMap);
            } else {
                propMap.put(id, occProps);
            }
        }
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
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_SPECIFIC_EPITHET : TARGET_TAXON_SPECIFIC_EPITHET, occurrenceProperties.get(DwcTerm.specificEpithet.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_GENUS : TARGET_TAXON_GENUS, occurrenceProperties.get(DwcTerm.genus.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_FAMILY : TARGET_TAXON_FAMILY, occurrenceProperties.get(DwcTerm.family.qualifiedName()));
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
                properties = parseJsonChunk(candidateJsonChunk);
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

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.getFields();
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
            TaxonUtil.enrichTaxonNames(properties);
        }
        return properties;
    }

    private static void setPropertyIfExists(Map<String, String> properties, JsonNode jsonNode, String remarkKey, String propertyName) {
        String value = jsonNode.has(remarkKey) ? jsonNode.get(remarkKey).asText() : null;
        if (StringUtils.isNotBlank(value)) {
            properties.put(propertyName, StringUtils.trim(value));
        }
    }


}
