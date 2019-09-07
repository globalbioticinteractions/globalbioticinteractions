package org.eol.globi.data;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.extensions.ExtensionProperty;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_SEX_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;

public class StudyImporterForDwCA extends StudyImporterWithListener {
    public static final String EXTENSION_ASSOCIATED_TAXA = "http://purl.org/NET/aec/associatedTaxa";
    public static final String EXTENSION_RESOURCE_RELATIONSHIP = "http://rs.tdwg.org/dwc/terms/ResourceRelationship";


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

            tmpDwA = Files.createTempDirectory("dwca");
            String archiveURL = getDataset().getOrDefault("url", archiveURI == null ? null : archiveURI.toString());
            Archive archive = DwCAUtil.archiveFor(getDataset().getResourceURI(archiveURL), tmpDwA.toString());
            InteractionListener interactionListener = getInteractionListener();

            String sourceCitation = getDataset().getCitation();

            importResourceRelationExtension(archive, interactionListener, sourceCitation);

            importAssociatedTaxaExtension(archive, interactionListener, sourceCitation);

            importCore(archive, interactionListener, sourceCitation);

        } catch (IOException e) {
            throw new StudyImporterException("failed to read archive [" + archiveURI + "]", e);
        } finally {
            if (tmpDwA != null) {
                org.apache.commons.io.FileUtils.deleteQuietly(tmpDwA.toFile());
            }
        }
    }

    public void importCore(Archive archive, InteractionListener interactionListener, String sourceCitation) throws StudyImporterException {
        for (Record rec : archive.getCore()) {
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
        String[] parts = StringUtils.splitByWholeSeparator(s, "|");
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
            String[] propertyValue = StringUtils.splitByWholeSeparator(part, ":", 2);
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

            final HTreeMap<String, Map<String, String>> occurrenceMap = db
                    .createHashMap("occurrenceMap")
                    .make();

            final Set<String> referencedSourceIds = db
                    .createHashSet("sourceIdMap")
                    .make();

            final Set<String> referencedTargetIds = db
                    .createHashSet("targetIdMap")
                    .make();


            for (Record record : resourceExtension) {
                String sourceId = record.value(DwcTerm.relatedResourceID);
                String targetId = record.value(DwcTerm.resourceID);
                if (StringUtils.isNotBlank(sourceId)
                        && StringUtils.isNotBlank(targetId)) {
                    referencedSourceIds.add(sourceId);
                    referencedTargetIds.add(targetId);
                }
            }

            ArchiveFile core = archive.getCore();
            for (Record coreRecord : core) {
                String id = coreRecord.value(DwcTerm.occurrenceID);
                if (referencedTargetIds.contains(id) || referencedSourceIds.contains(id)) {
                    TreeMap<String, String> occProps = new TreeMap<>();
                    termsToMap(coreRecord, occProps);
                    occurrenceMap.put(id, occProps);
                }
            }

            for (Record record : resourceExtension) {
                Map<String, String> props = new TreeMap<>();
                String sourceId = record.value(DwcTerm.relatedResourceID);
                String relationship = record.value(DwcTerm.relationshipOfResource);
                String targetId = record.value(DwcTerm.resourceID);

                final Map<String, String> relationshipOfResourceToInteractionTypeIdLookup = UnmodifiableMap.unmodifiableMap(new HashMap<String, String>() {{
                    put("Host to", InteractType.HOST_OF.getIRI());
                    put("Ectoparasite Of", InteractType.ECTOPARASITE_OF.getIRI());
                    put("Stomach Contents of", InteractType.EATEN_BY.getIRI());
                    put("Stomach Contents", InteractType.ATE.getIRI());
                }});

                if (StringUtils.isNotBlank(sourceId)
                        && StringUtils.isNotBlank(targetId)
                        && StringUtils.isNotBlank(relationship)
                        && relationshipOfResourceToInteractionTypeIdLookup.containsKey(relationship)) {

                    String sourceCitation1 = StringUtils.trim(sourceCitation);
                    props.put(STUDY_SOURCE_CITATION, sourceCitation1);
                    props.put(REFERENCE_CITATION, sourceCitation1);
                    props.put(REFERENCE_ID, sourceCitation1);
                    props.put(INTERACTION_TYPE_NAME, relationship);
                    props.put(INTERACTION_TYPE_ID, relationshipOfResourceToInteractionTypeIdLookup.get(relationship));
                    props.putIfAbsent(StudyImporterForMetaTable.EVENT_DATE, record.value(DwcTerm.relationshipEstablishedDate));

                    Map<String, String> sourceIdProperties = occurrenceMap.get(sourceId);
                    populateOccurrenceProperties(props, sourceId, true, sourceIdProperties);

                    Map<String, String> targetIdProperties = occurrenceMap.get(targetId);
                    populateOccurrenceProperties(props, targetId, false, targetIdProperties);

                    try {
                        interactionListener.newLink(props);
                    } catch (StudyImporterException e) {
                        //
                    }
                }
            }
        }
    }

    private static void populateOccurrenceProperties(Map<String, String> props, String occurrenceId, boolean isSource, Map<String, String> occurrenceProperties) {
        putIfAbsentAndNotBlank(props, isSource ? SOURCE_OCCURRENCE_ID : TARGET_OCCURRENCE_ID, occurrenceId);
        if (occurrenceProperties != null) {
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_TAXON_NAME : TARGET_TAXON_NAME, occurrenceProperties.get(DwcTerm.scientificName.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_SEX_NAME : TARGET_SEX_NAME, occurrenceProperties.get(DwcTerm.sex.qualifiedName()));
            putIfAbsentAndNotBlank(props, isSource ? SOURCE_LIFE_STAGE_NAME : SOURCE_LIFE_STAGE_NAME, occurrenceProperties.get(DwcTerm.lifeStage.qualifiedName()));
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
        ArchiveFile resourceRelationExtension = null;
        Set<ArchiveFile> extensions = archive.getExtensions();
        for (ArchiveFile extension : extensions) {
            if (StringUtils.equals(extension.getRowType().qualifiedName(),
                    EXTENSION_RESOURCE_RELATIONSHIP)) {
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
