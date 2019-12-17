package org.eol.globi.data;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.domain.InteractType;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.extensions.ExtensionProperty;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.file.ClosableIterator;
import org.globalbioticinteractions.dataset.DwCAUtil;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.eol.globi.data.StudyImporterForTSV.BASIS_OF_RECORD_NAME;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_SEX_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_SEX_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;

public class StudyImporterForDwCA extends StudyImporterWithListener {
    public static final String EXTENSION_ASSOCIATED_TAXA = "http://purl.org/NET/aec/associatedTaxa";
    public static final String EXTENSION_RESOURCE_RELATIONSHIP = "http://rs.tdwg.org/dwc/terms/ResourceRelationship";
    public static final String EXTENSION_TAXON = "http://rs.tdwg.org/dwc/terms/Taxon";


    public StudyImporterForDwCA(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        URI archiveURI = getDataset().getArchiveURI();
        Path tmpDwA = null;
        try {
            if (getDataset() == null) {
                throw new IllegalArgumentException("no dataset found");
            }

            String archiveURL = getDataset().getOrDefault("url", archiveURI == null ? null : archiveURI.toString());
            URI resourceURI = getDataset().getResourceURI(URI.create(archiveURL));
            if (resourceURI == null) {
                throw new StudyImporterException("failed to access DwC archive at [" + archiveURL + "]");
            }

            tmpDwA = Files.createTempDirectory("dwca");
            Archive archive = DwCAUtil.archiveFor(resourceURI, tmpDwA.toString());
            InteractionListener interactionListener = getInteractionListener();

            String sourceCitation = getDataset().getCitation();

            importResourceRelationExtension(archive, interactionListener, sourceCitation);

            importAssociatedTaxaExtension(archive, interactionListener, sourceCitation);

            importCore(archive, interactionListener, sourceCitation);

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

    public void importCore(Archive archive, InteractionListener interactionListener, String sourceCitation) throws StudyImporterException {
        ClosableIterator<Record> iterator = archive.getCore().iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                Record rec = iterator.next();
                handleRecord(interactionListener, sourceCitation, rec);
            } catch (IllegalStateException ex) {
                LogUtil.logError(getLogger(), "failed to handle dwc record", ex);
            }
        }
    }

    public void handleRecord(InteractionListener interactionListener, String sourceCitation, Record rec) throws StudyImporterException {
        List<Map<String, String>> interactionCandidates = new ArrayList<>();

        String associatedTaxa = rec.value(DwcTerm.associatedTaxa);
        if (StringUtils.isNotBlank(associatedTaxa)) {
            interactionCandidates.addAll(parseAssociatedTaxa(associatedTaxa));
        }

        String associatedOccurrences = rec.value(DwcTerm.associatedOccurrences);
        if (StringUtils.isNotBlank(associatedOccurrences)) {
            interactionCandidates.addAll(parseAssociatedOccurrences(associatedOccurrences));
        }

        String dynamicProperties = rec.value(DwcTerm.dynamicProperties);
        if (StringUtils.isNotBlank(dynamicProperties)) {
            interactionCandidates.add(parseDynamicProperties(dynamicProperties));
        }

        List<Map<String, String>> interactions = interactionCandidates
                .stream()
                .filter(x -> x.containsKey(INTERACTION_TYPE_ID) || x.containsKey(TARGET_TAXON_NAME) || x.containsKey(TARGET_OCCURRENCE_ID))
                .collect(Collectors.toList());

        logUnsupportedInteractionTypes(interactionCandidates, getLogger());


        Map<String, String> interaction = new HashMap<>(rec.terms().size());
        for (Term term : rec.terms()) {
            interaction.put(term.qualifiedName(), rec.value(term));
        }

        for (Map<String, String> interactionProperties : interactions) {
            interactionProperties.putAll(interaction);
            mapIfAvailable(rec, interactionProperties, BASIS_OF_RECORD_NAME, DwcTerm.basisOfRecord);
            mapCoreProperties(rec, interactionProperties, sourceCitation);
            interactionListener.newLink(interactionProperties);
        }
    }

    public static void mapCoreProperties(Record rec, Map<String, String> interactionProperties, String sourceCitation) {
        mapSourceProperties(rec, interactionProperties);
        mapLocationAndReferenceInfo(rec, interactionProperties, sourceCitation);
    }

    public static void mapLocationAndReferenceInfo(Record rec, Map<String, String> interactionProperties, String sourceCitation) {
        mapIfAvailable(rec, interactionProperties, LOCALITY_NAME, DwcTerm.locality);
        mapIfAvailable(rec, interactionProperties, LOCALITY_ID, DwcTerm.locationID);
        mapIfAvailable(rec, interactionProperties, DECIMAL_LONGITUDE, DwcTerm.decimalLongitude);
        mapIfAvailable(rec, interactionProperties, DECIMAL_LATITUDE, DwcTerm.decimalLatitude);
        mapIfAvailable(rec, interactionProperties, StudyImporterForMetaTable.EVENT_DATE, DwcTerm.eventDate);
        mapReferenceInfo(rec, interactionProperties);
        interactionProperties.put(STUDY_SOURCE_CITATION, sourceCitation);
    }

    public static void mapSourceProperties(Record rec, Map<String, String> interactionProperties) {
        mapIfAvailable(rec, interactionProperties, SOURCE_OCCURRENCE_ID, DwcTerm.occurrenceID);
        mapIfAvailable(rec, interactionProperties, StudyImporterForTSV.SOURCE_COLLECTION_CODE, DwcTerm.collectionCode);
        mapIfAvailable(rec, interactionProperties, StudyImporterForTSV.SOURCE_COLLECTION_ID, DwcTerm.collectionID);
        mapIfAvailable(rec, interactionProperties, StudyImporterForTSV.SOURCE_INSTITUTION_CODE, DwcTerm.institutionCode);
        mapIfAvailable(rec, interactionProperties, StudyImporterForTSV.SOURCE_CATALOG_NUMBER, DwcTerm.catalogNumber);
        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_NAME, DwcTerm.scientificName);
        mapIfAvailable(rec, interactionProperties, SOURCE_LIFE_STAGE_NAME, DwcTerm.lifeStage);
        mapIfAvailable(rec, interactionProperties, SOURCE_SEX_NAME, DwcTerm.sex);
    }

    private static void mapReferenceInfo(Record rec, Map<String, String> interactionProperties) {
        String value = StringUtils.trim(rec.value(DcTerm.references));
        if (StringUtils.isNotBlank(value)) {
            interactionProperties.put(REFERENCE_CITATION, value);
            interactionProperties.put(REFERENCE_ID, value);
            try {
                URI referenceURI = new URI(value);
                URL url = referenceURI.toURL();
                interactionProperties.put(REFERENCE_URL, url.toString());
            } catch (MalformedURLException | URISyntaxException e) {
                // opportunistic extraction of url from references to take advantage of practice used in Symbiota)
            }
        }
    }

    static void logUnsupportedInteractionTypes(List<Map<String, String>> interactionCandidates, final ImportLogger logger) {
        interactionCandidates
                .stream()
                .filter(x -> !x.containsKey(INTERACTION_TYPE_ID) && x.containsKey(INTERACTION_TYPE_NAME))
                .map(x -> x.get(INTERACTION_TYPE_NAME))
                .forEach(x -> {
                    if (logger != null) {
                        logger.warn(null, "found unsupported interaction type [" + x + "]");
                    }
                });
    }

    public static void mapIfAvailable(Record rec, Map<String, String> interactionProperties, String key, Term term) {
        String value = rec.value(term);
        mapIfAvailable(interactionProperties, key, value);
    }

    public static void mapIfAvailable(Map<String, String> interactionProperties, String key, String value) {
        if ((StringUtils.isNotBlank(value))) {
            interactionProperties.put(key, value);
        }
    }

    static List<Map<String, String>> parseAssociatedTaxa(String s) {
        List<Map<String, String>> properties = new ArrayList<>();
        String[] parts = StringUtils.split(s, "|;,");
        for (String part : parts) {
            String[] verbTaxon = StringUtils.splitByWholeSeparator(part, ":", 2);
            if (verbTaxon.length == 2) {
                addSpecificInteractionForAssociatedTaxon(properties, verbTaxon);
            } else {
                addDefaultInteractionForAssociatedTaxon(properties, part);
            }
        }
        return properties;
    }

    private static void addSpecificInteractionForAssociatedTaxon(List<Map<String, String>> properties, String[] verbTaxon) {
        HashMap<String, String> e = new HashMap<>();
        String interactionTypeName = StringUtils.lowerCase(StringUtils.trim(verbTaxon[0]));
        e.put(INTERACTION_TYPE_NAME, interactionTypeName);
        InteractType interactType = InteractType.typeOf(interactionTypeName);
        if (interactType != null) {
            e.put(INTERACTION_TYPE_ID, interactType.getIRI());
        }
        e.put(TARGET_TAXON_NAME, StringUtils.trim(verbTaxon[1]));
        properties.add(e);
    }

    private static void addDefaultInteractionForAssociatedTaxon(List<Map<String, String>> properties, String part) {
        properties.add(new HashMap<String, String>() {{
            put(TARGET_TAXON_NAME, part);
            put(INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());
            put(INTERACTION_TYPE_NAME, InteractType.INTERACTS_WITH.getLabel());
        }});
    }

    static List<Map<String, String>> parseAssociatedOccurrences(String s) {
        List<Map<String, String>> propertyList = new ArrayList<>();

        Map<String, InteractType> mapping = new TreeMap<String, InteractType>() {
            {
                put("(ate)", InteractType.ATE);
                put("(eaten by)", InteractType.EATEN_BY);
                put("(parasite of)", InteractType.PARASITE_OF);
                put("(host of)", InteractType.HOST_OF);
            }
        };

        String[] relationships = StringUtils.split(s, ";");
        for (String relationship : relationships) {
            String relationshipTrimmed = StringUtils.trim(relationship);
            for (Map.Entry<String, InteractType> mapEntry : mapping.entrySet()) {
                if (StringUtils.startsWith(relationshipTrimmed, mapEntry.getKey())) {
                    String targetCollectionAndOccurrenceId = StringUtils.trim(StringUtils.substring(relationshipTrimmed, mapEntry.getKey().length()));
                    int i = StringUtils.indexOf(targetCollectionAndOccurrenceId, " ");
                    if (i > -1) {
                        String occurrenceId = StringUtils.substring(targetCollectionAndOccurrenceId, i);
                        if (StringUtils.isNotBlank(occurrenceId)) {
                            TreeMap<String, String> properties = new TreeMap<>();
                            properties.put(TARGET_OCCURRENCE_ID, StringUtils.trim(occurrenceId));
                            properties.put(INTERACTION_TYPE_ID, mapEntry.getValue().getIRI());
                            properties.put(INTERACTION_TYPE_NAME, mapEntry.getValue().getLabel());
                            propertyList.add(properties);
                        }
                    }
                }
            }

        }
        return propertyList;
    }

    static Map<String, String> parseDynamicProperties(String s) {
        Map<String, String> properties = new HashMap<>();
        String[] parts = StringUtils.splitByWholeSeparator(s, ";");
        for (String part : parts) {
            String[] propertyValue = StringUtils.split(part, ":=", 2);
            if (propertyValue.length == 2) {
                properties.put(StringUtils.trim(propertyValue[0]), StringUtils.trim(propertyValue[1]));
            }
        }
        return properties;
    }

    static void importAssociatedTaxaExtension(Archive archive, InteractionListener interactionListener, String sourceCitation) {
        if (hasAssociatedTaxaExtension(archive)) {
            ArchiveFile extension = archive.getExtension(new ExtensionProperty(EXTENSION_ASSOCIATED_TAXA));
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

                        mapCoreProperties(coreRecord, interaction, sourceCitation);

                        interactionListener.newLink(interaction);
                    } catch (StudyImporterException e) {
                        //
                    }
                }
            }
        }
    }

    static void importResourceRelationExtension(Archive archive, InteractionListener interactionListener, String sourceCitation) {

        ArchiveFile resourceExtension = findResourceRelationshipExtension(archive);

        if (resourceExtension != null) {

            DB db = DBMaker
                    .newMemoryDirectDB()
                    .compressionEnable()
                    .transactionDisable()
                    .make();

            final HTreeMap<String, Map<String, Map<String, String>>> termIdPropMap = db
                    .createHashMap("termIdPropMap")
                    .make();

            final Set<String> referencedSourceIds = db
                    .createHashSet("sourceIdMap")
                    .make();

            final Set<String> referencedTargetIds = db
                    .createHashSet("targetIdMap")
                    .make();


            for (Record record : resourceExtension) {
                String targetId = record.value(DwcTerm.relatedResourceID);
                String sourceId = record.value(DwcTerm.resourceID);
                if (StringUtils.isNotBlank(sourceId)
                        && StringUtils.isNotBlank(targetId)) {
                    referencedSourceIds.add(sourceId);
                    referencedTargetIds.add(targetId);
                }
            }
            final List<DwcTerm> idTerms = Arrays.asList(
                    DwcTerm.occurrenceID, DwcTerm.taxonID);

            List<ArchiveFile> archiveFiles = new ArrayList<>();
            archiveFiles.add(archive.getCore());

            ArchiveFile taxon = findResourceExtension(archive, EXTENSION_TAXON);
            if (taxon != null) {
                archiveFiles.add(taxon);
            }

            for (ArchiveFile archiveFile : archiveFiles) {
                for (Record record : archiveFile) {
                    for (DwcTerm idTerm : idTerms) {
                        attemptLinkUsingTerm(termIdPropMap,
                                referencedSourceIds,
                                referencedTargetIds,
                                record,
                                idTerm);
                    }
                }
            }


            for (Record record : resourceExtension) {
                Map<String, String> props = new TreeMap<>();
                String sourceId = record.value(DwcTerm.resourceID);
                String relationship = record.value(DwcTerm.relationshipOfResource);

                Optional<Term> relationshipOfResourceIDTerm = record.terms().stream().filter(x -> StringUtils.equals(x.simpleName(), "relationshipOfResourceID")).findFirst();
                String relationshipTypeIdValue = relationshipOfResourceIDTerm
                        .map(record::value)
                        .orElse(null);
                String targetId = record.value(DwcTerm.relatedResourceID);

                String relationshipTypeId = StringUtils.isBlank(relationshipTypeIdValue)
                        ? findRelationshipTypeIdByLabel(relationship)
                        : relationshipTypeIdValue;

                if (StringUtils.isNotBlank(sourceId)
                        && StringUtils.isNotBlank(targetId)
                        && StringUtils.isNotBlank(relationshipTypeId)) {

                    String sourceCitationTrimmed = StringUtils.trim(sourceCitation);
                    props.put(STUDY_SOURCE_CITATION, sourceCitationTrimmed);

                    String relationshipAccordingTo = record.value(DwcTerm.relationshipAccordingTo);
                    String referenceCitation = StringUtils.isBlank(relationshipAccordingTo)
                            ? sourceCitationTrimmed
                            : relationshipAccordingTo;
                    props.putIfAbsent(REFERENCE_CITATION, referenceCitation);
                    props.put(REFERENCE_ID, sourceCitationTrimmed);

                    props.put(INTERACTION_TYPE_NAME, relationship);
                    props.put(INTERACTION_TYPE_ID, relationshipTypeId);
                    props.putIfAbsent(StudyImporterForMetaTable.EVENT_DATE, record.value(DwcTerm.relationshipEstablishedDate));

                    for (DwcTerm idTerm : idTerms) {
                        if (termIdPropMap.containsKey(idTerm.qualifiedName())) {
                            Map<String, Map<String, String>> propMap = termIdPropMap.get(idTerm.qualifiedName());
                            Map<String, String> sourceIdProperties = propMap.get(sourceId);

                            Pair<String, String> idLabelPair = Pair.of(
                                    SOURCE_OCCURRENCE_ID,
                                    TARGET_OCCURRENCE_ID);

                            if (idTerm.equals(DwcTerm.taxonID)) {
                                idLabelPair = Pair.of(SOURCE_TAXON_ID, TARGET_TAXON_ID);
                            }

                            populatePropertiesAssociatedWithId(props, sourceId, true, sourceIdProperties, idLabelPair);

                            Map<String, String> targetIdProperties = propMap.get(targetId);
                            populatePropertiesAssociatedWithId(props, targetId, false, targetIdProperties, idLabelPair);
                        }

                    }

                    try {
                        interactionListener.newLink(props);
                    } catch (StudyImporterException e) {
                        //
                    }
                }
            }
        }
    }

    private static String findRelationshipTypeIdByLabel(String relationship) {
        final Map<String, String> relationshipOfResourceToInteractionTypeIdLookup
                = UnmodifiableMap.unmodifiableMap(new HashMap<String, String>() {{
            put("associated with", InteractType.INTERACTS_WITH.getIRI());
            put("ex", InteractType.HAS_HOST.getIRI());
            put("host to", InteractType.HOST_OF.getIRI());
            put("host", InteractType.HAS_HOST.getIRI());
            put("h", InteractType.HAS_HOST.getIRI());
            put("larval foodplant", InteractType.ATE.getIRI());
            put("ectoparasite of", InteractType.ECTOPARASITE_OF.getIRI());
            put("parasite of", InteractType.PARASITE_OF.getIRI());
            put("stomach contents of", InteractType.EATEN_BY.getIRI());
            put("stomach contents", InteractType.ATE.getIRI());
            put("eaten by", InteractType.EATEN_BY.getIRI());
        }});

        String relationshipKey = StringUtils.lowerCase(StringUtils.trim(relationship));

        return StringUtils.isBlank(relationship)
                ? null
                : relationshipOfResourceToInteractionTypeIdLookup.get(relationshipKey);
    }

    private static void attemptLinkUsingTerm(HTreeMap<String, Map<String, Map<String, String>>> termIdPropertyMap,
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
            putIfAbsentAndNotBlank(props, isSource ? StudyImporterForTSV.SOURCE_INSTITUTION_CODE : StudyImporterForTSV.TARGET_INSTITUTION_CODE, occurrenceProperties.get(DwcTerm.institutionCode.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? StudyImporterForTSV.SOURCE_COLLECTION_ID : StudyImporterForTSV.TARGET_COLLECTION_ID, occurrenceProperties.get(DwcTerm.collectionID.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? StudyImporterForTSV.SOURCE_COLLECTION_CODE : StudyImporterForTSV.TARGET_COLLECTION_CODE, occurrenceProperties.get(DwcTerm.collectionCode.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? StudyImporterForTSV.SOURCE_CATALOG_NUMBER : StudyImporterForTSV.TARGET_CATALOG_NUMBER, occurrenceProperties.get(DwcTerm.catalogNumber.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_ID : TARGET_TAXON_ID, occurrenceProperties.get(DwcTerm.taxonID.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_NAME : TARGET_TAXON_NAME, occurrenceProperties.get(DwcTerm.scientificName.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_SEX_NAME : TARGET_SEX_NAME, occurrenceProperties.get(DwcTerm.sex.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_LIFE_STAGE_NAME : TARGET_LIFE_STAGE_NAME, occurrenceProperties.get(DwcTerm.lifeStage.qualifiedName()));
            putIfAbsentAndNotBlank(props, LOCALITY_NAME, occurrenceProperties.get(DwcTerm.locality.qualifiedName()));
            putIfAbsentAndNotBlank(props, DECIMAL_LATITUDE, occurrenceProperties.get(DwcTerm.decimalLatitude.qualifiedName()));
            putIfAbsentAndNotBlank(props, DECIMAL_LONGITUDE, occurrenceProperties.get(DwcTerm.decimalLongitude.qualifiedName()));
            putIfAbsentAndNotBlank(props, StudyImporterForMetaTable.EVENT_DATE, occurrenceProperties.get(DwcTerm.eventDate.qualifiedName()));
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

    private static ArchiveFile findResourceExtension(Archive archive, String extensionType) {
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
        StudyImporterForDwCA.mapIfAvailable(
                interaction,
                StudyImporterForTSV.BASIS_OF_RECORD_NAME,
                targetProperties.get("http://rs.tdwg.org/dwc/terms/basisOfRecord")
        );

        String scientificName = targetProperties.get("http://purl.org/NET/aec/associatedScientificName");
        String specificEpithet = targetProperties.get("http://purl.org/NET/aec/associatedSpecificEpithet");
        String genus = targetProperties.get("http://purl.org/NET/aec/associatedGenus");

        String targetName = (StringUtils.isNotBlank(genus) && StringUtils.isNotBlank(specificEpithet))
                ? StringUtils.join(new String[]{genus, specificEpithet}, " ")
                : scientificName;

        StudyImporterForDwCA.mapIfAvailable(
                interaction,
                StudyImporterForTSV.TARGET_TAXON_NAME,
                targetName
        );

        StudyImporterForDwCA.mapIfAvailable(
                interaction,
                StudyImporterForTSV.TARGET_TAXON_NAME,
                targetProperties.get("http://purl.org/NET/aec/associatedScientificName")
        );

        StudyImporterForDwCA.mapIfAvailable(
                interaction,
                StudyImporterForTSV.INTERACTION_TYPE_ID,
                targetProperties.get("http://purl.org/NET/aec/associatedRelationshipURI")
        );

        StudyImporterForDwCA.mapIfAvailable(
                interaction,
                StudyImporterForTSV.INTERACTION_TYPE_NAME,
                targetProperties.get("http://purl.org/NET/aec/associatedRelationshipTerm")
        );
    }

    static boolean hasExtension(Archive archive, String extensionQualitfiedName) {
        boolean hasExtension = false;
        Set<ArchiveFile> extensions = archive.getExtensions();
        for (ArchiveFile extension : extensions) {
            Term rowType = extension.getRowType();
            if (rowType != null && StringUtils.equals(extensionQualitfiedName, rowType.qualifiedName())) {
                hasExtension = true;
            }
        }
        return hasExtension;

    }

    static boolean hasAssociatedTaxaExtension(Archive archive) {
        return hasExtension(archive, EXTENSION_ASSOCIATED_TAXA);
    }

    static boolean hasResourceRelationships(Archive archive) {
        return hasExtension(archive, EXTENSION_RESOURCE_RELATIONSHIP);
    }


}
