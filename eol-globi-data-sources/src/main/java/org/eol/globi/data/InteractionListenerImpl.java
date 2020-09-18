package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.InvalidLocationException;
import org.gbif.dwc.terms.DwcTerm;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.DatasetImporterForTSV.ARGUMENT_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.BASIS_OF_RECORD_ID;
import static org.eol.globi.data.DatasetImporterForTSV.BASIS_OF_RECORD_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_BODY_PART_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_BODY_PART_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_LIFE_STAGE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_SEX_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_SEX_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_LIFE_STAGE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_LIFE_STAGE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_SEX_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_SEX_NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.CATALOG_NUMBER;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.COLLECTION_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.INSTITUTION_CODE;
import static org.eol.globi.domain.PropertyAndValueDictionary.OCCURRENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_RANK;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_RANK;

class InteractionListenerImpl implements InteractionListener {
    private static final String[] LOCALITY_ID_TERMS = {LOCALITY_ID, DwcTerm.locationID.normQName};
    private static final String[] LOCALITY_NAME_TERMS = {LOCALITY_NAME, DwcTerm.locality.normQName, DwcTerm.verbatimLocality.normQName};
    private final NodeFactory nodeFactory;
    private final GeoNamesService geoNamesService;

    private final ImportLogger logger;

    public InteractionListenerImpl(NodeFactory nodeFactory, GeoNamesService geoNamesService, ImportLogger logger) {
        this.nodeFactory = nodeFactory;
        this.geoNamesService = geoNamesService;
        this.logger = logger;
    }

    @Override
    public void newLink(Map<String, String> link) throws StudyImporterException {
        try {
            List<Map<String, String>> propertiesList = AssociatedTaxaUtil.expandIfNeeded(link);
            for (Map<String, String> expandedLink : propertiesList) {
                addPlaceholderNamesIfNeeded(expandedLink);

                if (expandedLink != null && validLink(expandedLink)) {
                    logIfPossible(expandedLink, "biotic interaction found");
                    importValidLink(expandedLink);
                }
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import: " + link, e);
        }
    }

    public void addPlaceholderNamesIfNeeded(Map<String, String> expandedLink) {
        if (createSourceTaxonPredicate(null).negate().test(expandedLink)) {
            Stream<String> placeholderNames = Stream.of(
                    SOURCE_INSTITUTION_CODE,
                    SOURCE_COLLECTION_CODE,
                    SOURCE_COLLECTION_ID,
                    SOURCE_CATALOG_NUMBER,
                    SOURCE_OCCURRENCE_ID);
            addPlaceholderNamesIfPossible(expandedLink, placeholderNames, "source", SOURCE_TAXON_NAME);
        }
        if (createTargetTaxonPredicate(null).negate().test(expandedLink)) {
            Stream<String> placeholderNames = Stream.of(
                    TARGET_INSTITUTION_CODE,
                    TARGET_COLLECTION_CODE,
                    TARGET_COLLECTION_ID,
                    TARGET_CATALOG_NUMBER,
                    TARGET_OCCURRENCE_ID);
            addPlaceholderNamesIfPossible(expandedLink, placeholderNames, "target", TARGET_TAXON_NAME);
        }
    }

    public void addPlaceholderNamesIfPossible(Map<String, String> expandedLink, Stream<String> placeholderNames, String sourceOrTarget, String nameToBeFilled) {
        final String placeholderMessage = " using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder";

        String targetNamePlaceholder = placeholderNames
                .map(expandedLink::get)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
        if (StringUtils.isNotBlank(targetNamePlaceholder)) {
            expandedLink.putIfAbsent(nameToBeFilled, targetNamePlaceholder);
            logWarningIfPossible(expandedLink, sourceOrTarget + " taxon name missing:" + placeholderMessage);
        }
    }

    private void logIfPossible(Map<String, String> expandedProperties, String msg) {
        if (logger != null) {
            logger.info(LogUtil.contextFor(expandedProperties), msg);
        }
    }

    private boolean validLink(Map<String, String> link) {
        return validLink(link, getLogger());
    }

    static boolean validLink(Map<String, String> link, ImportLogger logger) {
        Predicate<Map<String, String>> hasSourceTaxon = createSourceTaxonPredicate(logger);

        Predicate<Map<String, String>> hasTargetTaxon = createTargetTaxonPredicate(logger);

        Predicate<Map<String, String>> hasInteractionType = createInteractionTypePredicate(logger);

        Predicate<Map<String, String>> hasReferenceId = createReferencePredicate(logger);

        return hasSourceTaxon
                .and(hasTargetTaxon)
                .and(hasInteractionType)
                .and(hasReferenceId)
                .test(link);
    }

    private static Predicate<Map<String, String>> createSourceTaxonPredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String sourceTaxonName = l.get(SOURCE_TAXON_NAME);
            String sourceTaxonId = l.get(SOURCE_TAXON_ID);
            boolean isValid = StringUtils.isNotBlank(sourceTaxonName) || StringUtils.isNotBlank(sourceTaxonId);
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "source taxon name missing");
            }
            return isValid;
        };
    }

    private static Predicate<Map<String, String>> createTargetTaxonPredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String targetTaxonName = l.get(TARGET_TAXON_NAME);
            String targetTaxonId = l.get(TARGET_TAXON_ID);

            boolean isValid = StringUtils.isNotBlank(targetTaxonName) || StringUtils.isNotBlank(targetTaxonId);
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "target taxon name missing");
            }
            return isValid;
        };
    }

    private static Predicate<Map<String, String>> createReferencePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            boolean isValid = StringUtils.isNotBlank(l.get(REFERENCE_ID));
            if (!isValid && logger != null) {
                logger.warn(LogUtil.contextFor(l), "missing [" + REFERENCE_ID + "]");
            }
            return isValid;
        };
    }

    static Predicate<Map<String, String>> createInteractionTypePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String interactionTypeId = l.get(INTERACTION_TYPE_ID);
            boolean hasValidId = false;
            if (StringUtils.isBlank(interactionTypeId) && logger != null) {
                if (StringUtils.isBlank(l.get(INTERACTION_TYPE_NAME))) {
                    logger.warn(LogUtil.contextFor(l), "missing interaction type");
                } else {
                    logger.warn(LogUtil.contextFor(l), "found unsupported interaction type with name: [" + l.get(INTERACTION_TYPE_NAME) + "]");
                }
            } else {
                hasValidId = InteractType.typeOf(interactionTypeId) != null;
                if (!hasValidId && logger != null) {
                    StringBuilder msg = new StringBuilder("found unsupported interaction type with id: [" + interactionTypeId + "]");
                    if (StringUtils.isNotBlank(l.get(INTERACTION_TYPE_NAME))) {
                        msg.append(" and name: [")
                                .append(l.get(INTERACTION_TYPE_NAME))
                                .append("]");
                    }
                    logger.warn(LogUtil.contextFor(l), msg.toString());
                }
            }
            return hasValidId;
        };
    }

    private void importValidLink(Map<String, String> link) throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy(studyFromLink(link));

        Specimen source = createSpecimen(
                link,
                study,
                SOURCE_TAXON_NAME,
                SOURCE_TAXON_ID,
                SOURCE_BODY_PART_NAME,
                SOURCE_BODY_PART_ID,
                SOURCE_LIFE_STAGE_NAME,
                SOURCE_LIFE_STAGE_ID,
                SOURCE_TAXON_PATH,
                SOURCE_TAXON_PATH_NAMES,
                SOURCE_SEX_NAME,
                SOURCE_SEX_ID,
                SOURCE_TAXON_RANK,
                SOURCE_TAXON_PATH_IDS);

        setExternalIdNotBlank(link, SOURCE_OCCURRENCE_ID, source);
        setPropertyIfAvailable(link, source, SOURCE_OCCURRENCE_ID, OCCURRENCE_ID);
        setPropertyIfAvailable(link, source, SOURCE_CATALOG_NUMBER, CATALOG_NUMBER);
        setPropertyIfAvailable(link, source, SOURCE_COLLECTION_CODE, COLLECTION_CODE);
        setPropertyIfAvailable(link, source, SOURCE_COLLECTION_ID, COLLECTION_ID);
        setPropertyIfAvailable(link, source, SOURCE_INSTITUTION_CODE, INSTITUTION_CODE);

        Specimen target = createSpecimen(
                link,
                study,
                TARGET_TAXON_NAME,
                TARGET_TAXON_ID,
                TARGET_BODY_PART_NAME,
                TARGET_BODY_PART_ID,
                TARGET_LIFE_STAGE_NAME,
                TARGET_LIFE_STAGE_ID,
                TARGET_TAXON_PATH,
                TARGET_TAXON_PATH_NAMES,
                TARGET_SEX_NAME,
                TARGET_SEX_ID,
                TARGET_TAXON_RANK,
                TARGET_TAXON_PATH_IDS);

        setExternalIdNotBlank(link, TARGET_OCCURRENCE_ID, target);
        setPropertyIfAvailable(link, target, TARGET_OCCURRENCE_ID, OCCURRENCE_ID);
        setPropertyIfAvailable(link, target, TARGET_CATALOG_NUMBER, CATALOG_NUMBER);
        setPropertyIfAvailable(link, target, TARGET_COLLECTION_CODE, COLLECTION_CODE);
        setPropertyIfAvailable(link, target, TARGET_COLLECTION_ID, COLLECTION_ID);
        setPropertyIfAvailable(link, target, TARGET_INSTITUTION_CODE, INSTITUTION_CODE);


        String interactionTypeId = link.get(INTERACTION_TYPE_ID);
        InteractType type = InteractType.typeOf(interactionTypeId);

        source.interactsWith(target, type, getOrCreateLocation(link));
    }

    private void setPropertyIfAvailable(Map<String, String> link, Specimen source, String key, String propertyKey) {
        String value = link.get(key);
        if (StringUtils.isNotBlank(value)) {
            source.setProperty(propertyKey, value);
        }
    }

    private void setExternalIdNotBlank(Map<String, String> link, String sourceOccurrenceId, Specimen source1) {
        String s = link.get(sourceOccurrenceId);
        if (StringUtils.isNotBlank(s)) {
            source1.setExternalId(s);
        }
    }

    private Specimen createSpecimen(Map<String, String> link,
                                    Study study,
                                    String taxonNameLabel,
                                    String taxonIdLabel,
                                    String bodyPartName,
                                    String bodyPartId,
                                    String lifeStageName,
                                    String lifeStageId,
                                    String taxonPathLabel,
                                    String taxonPathNamesLabel,
                                    String sexLabel,
                                    String sexId, String taxonRankLabel, String taxonPathIdsLabel) throws StudyImporterException {
        String argumentTypeId = link.get(ARGUMENT_TYPE_ID);
        RelTypes[] argumentType = refutes(argumentTypeId)
                ? new RelTypes[]{RelTypes.REFUTES}
                : new RelTypes[]{RelTypes.COLLECTED, RelTypes.SUPPORTS};

        String sourceTaxonName = link.get(taxonNameLabel);
        String sourceTaxonId = link.get(taxonIdLabel);
        TaxonImpl taxon = new TaxonImpl(sourceTaxonName, sourceTaxonId);

        String taxonRank = link.get(taxonRankLabel);
        if (StringUtils.isNotBlank(taxonRank)) {
            taxon.setRank(taxonRank);
        }


        String taxonPath = link.get(taxonPathLabel);
        if (StringUtils.isNotBlank(taxonPath)) {
            taxon.setPath(taxonPath);
        }

        String taxonPathIds = link.get(taxonPathIdsLabel);
        if (StringUtils.isNotBlank(taxonPathIds)) {
            taxon.setPathIds(taxonPathIds);
        }

        String taxonPathNames = link.get(taxonPathNamesLabel);
        if (StringUtils.isNotBlank(taxonPathNames)) {
            taxon.setPathNames(taxonPathNames);
        }

        Specimen source = nodeFactory.createSpecimen(study, taxon, argumentType);
        setBasisOfRecordIfAvailable(link, source);
        setDateTimeIfAvailable(link, source);
        setBodyPartIfAvailable(link, source, bodyPartName, bodyPartId);
        setLifeStageIfAvailable(link, source, lifeStageName, lifeStageId);
        setSexIfAvailable(link, source, sexLabel, sexId);
        return source;
    }

    private boolean refutes(String argumentTypeId) {
        return StringUtils.equalsIgnoreCase(argumentTypeId, PropertyAndValueDictionary.REFUTES);
    }

    private void setLifeStageIfAvailable(Map<String, String> link, Specimen source, String name, String id) {
        final String lifeStageName = link.get(name);
        final String lifeStageId = link.get(id);
        if (StringUtils.isNotBlank(lifeStageName) || StringUtils.isNotBlank(lifeStageId)) {
            source.setLifeStage(new TermImpl(lifeStageId, lifeStageName));
        }
    }

    private void setSexIfAvailable(Map<String, String> link, Specimen source, String name, String id) {
        final String sexName = link.get(name);
        final String sexId = link.get(id);
        if (StringUtils.isNotBlank(sexName) || StringUtils.isNotBlank(sexId)) {
            source.setSex(new TermImpl(sexId, sexName));
        }
    }

    private StudyImpl studyFromLink(Map<String, String> l) {
        String referenceCitation = l.get(REFERENCE_CITATION);
        DOI doi = null;
        String doiString = l.get(REFERENCE_DOI);
        try {
            doi = StringUtils.isBlank(doiString) ? null : DOI.create(doiString);
        } catch (MalformedDOIException e) {
            if (logger != null) {
                logger.warn(LogUtil.contextFor(l), "found malformed doi [" + doiString + "]");
            }
        }
        StudyImpl study1 = new StudyImpl(l.get(REFERENCE_ID),
                doi,
                referenceCitation);

        final String referenceUrl = l.get(REFERENCE_URL);
        if (StringUtils.isBlank(study1.getExternalId()) && StringUtils.isNotBlank(referenceUrl)) {
            study1.setExternalId(referenceUrl);
        }

        return study1;
    }

    private void setDateTimeIfAvailable(Map<String, String> link, Specimen target) throws StudyImporterException {
        final String eventDate = link.get(DatasetImporterForMetaTable.EVENT_DATE);
        if (StringUtils.isNotBlank(eventDate)) {
            try {
                final DateTime dateTime = DateUtil
                        .parseDateUTC(applySymbiotaDateTimeFix(eventDate));
                Date date = dateTime.toDate();
                if (dateTime.getYear() == 8888) {
                    // 8888 is a magic number used by Arctos
                    // see http://handbook.arctosdb.org/documentation/dates.html#restricted-data
                    // https://github.com/ArctosDB/arctos/issues/2426
                    logWarningIfPossible(link, "date [" + DateUtil.printDate(date) + "] appears to be restricted, see http://handbook.arctosdb.org/documentation/dates.html#restricted-data");
                } else if (date.after(new Date())) {
                    logWarningIfPossible(link, "date [" + DateUtil.printDate(date) + "] is in the future");
                } else if (dateTime.getYear() < 100) {
                    logWarningIfPossible(link, "date [" + DateUtil.printDate(date) + "] occurred in the first century AD");
                } else if (StringUtils.split(eventDate, "/").length > 1) {
                    DateTime endDate = DateUtil.parseDateUTC(StringUtils.split(eventDate, "/")[1]);
                    if (dateTime.isAfter(endDate)) {
                        logWarningIfPossible(link, "date range [" + eventDate + "] appears to start after it ends.");
                    }
                }
                nodeFactory.setUnixEpochProperty(target, date);
            } catch (IllegalArgumentException ex) {
                logWarningIfPossible(link, "invalid date string [" + eventDate + "]");
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to set time for [" + eventDate + "]", e);
            }

        }

    }

    private static String applySymbiotaDateTimeFix(String eventDate) {
        String eventDateCorrected = eventDate;
        if (StringUtils.contains(eventDate, "-00")) {
            // see https://github.com/globalbioticinteractions/scan/issues/2
            // see http://symbiota.org/docs/symbiota-occurrence-data-fields-2/#eventDate
            String[] parts = StringUtils.splitByWholeSeparator(eventDate, "-00");
            eventDateCorrected = parts[0];
        }
        return eventDateCorrected;
    }

    private void setBasisOfRecordIfAvailable(Map<String, String> link, Specimen specimen) {
        final String basisOfRecordName = link.get(BASIS_OF_RECORD_NAME);
        final String basisOfRecordId = link.get(BASIS_OF_RECORD_ID);
        if (StringUtils.isNotBlank(basisOfRecordName) || StringUtils.isNotBlank(basisOfRecordId)) {
            specimen.setBasisOfRecord(new TermImpl(basisOfRecordId, basisOfRecordName));
        }
    }

    private void setBodyPartIfAvailable(Map<String, String> link, Specimen specimen, String name, String id) {
        final String bodyPartName = link.get(name);
        final String bodyPartId = link.get(id);
        if (StringUtils.isNotBlank(bodyPartName) || StringUtils.isNotBlank(bodyPartId)) {
            specimen.setBodyPart(new TermImpl(bodyPartId, bodyPartName));
        }
    }

    private Location getOrCreateLocation(Map<String, String> link) throws NodeFactoryException {
        LatLng centroid = null;
        String[] latitudes = {DECIMAL_LATITUDE, DatasetImporterForMetaTable.LATITUDE};
        String latitude = getFirstValueForTerms(link, latitudes);

        String[] longitudes = {DECIMAL_LONGITUDE, DatasetImporterForMetaTable.LONGITUDE};
        String longitude = getFirstValueForTerms(link, longitudes);

        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                centroid = LocationUtil.parseLatLng(latitude, longitude);
            } catch (InvalidLocationException e) {
                logWarningIfPossible(link, "found invalid location: [" + e.getMessage() + "]");
            }
        }
        String localityId = getFirstValueForTerms(link, LOCALITY_ID_TERMS);

        if (centroid == null) {
            if (StringUtils.isNotBlank(localityId)) {
                try {
                    centroid = getGeoNamesService().findLatLng(localityId);
                } catch (IOException ex) {
                    String message = "failed to lookup [" + localityId + "] because of: [" + ex.getMessage() + "]";
                    logWarningIfPossible(link, message);
                }
            }
        }

        LocationImpl location = null;
        if (centroid != null) {
            location = new LocationImpl(centroid.getLat(),
                    centroid.getLng(), null, null);
        } else if (StringUtils.isNotBlank(localityId) || StringUtils.isNotBlank(LOCALITY_NAME)) {
            location = new LocationImpl(null,
                    null, null, null);
        }

        if (location != null) {
            if (StringUtils.isNotBlank(localityId)) {
                location.setLocalityId(localityId);
            }
            String localityName = getFirstValueForTerms(link, LOCALITY_NAME_TERMS);
            if (StringUtils.isNotBlank(localityName)) {
                location.setLocality(localityName);
            }
        }
        return location == null ? null : nodeFactory.getOrCreateLocation(location);
    }

    private void logWarningIfPossible(Map<String, String> link, String message) {
        logWarningIfPossible(link, message, getLogger());
    }

    private static void logWarningIfPossible(Map<String, String> link, String message, ImportLogger logger) {
        if (logger != null) {
            logger.warn(LogUtil.contextFor(link), message);
        }
    }

    private String getFirstValueForTerms(Map<String, String> link, String[] latitudes) {
        String latitude = null;
        for (String latitudeTerm : latitudes) {
            if (StringUtils.isBlank(latitude)) {
                latitude = link.get(latitudeTerm);
            }
        }
        return latitude;
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }

    public ImportLogger getLogger() {
        return logger;
    }


}
