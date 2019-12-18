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
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.InvalidLocationException;
import org.gbif.dwc.terms.DwcTerm;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.eol.globi.data.StudyImporterForTSV.ARGUMENT_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.BASIS_OF_RECORD_ID;
import static org.eol.globi.data.StudyImporterForTSV.BASIS_OF_RECORD_NAME;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_LIFE_STAGE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_LIFE_STAGE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;

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
    public void newLink(Map<String, String> properties) throws StudyImporterException {
        try {
            if (properties != null && validLink(properties)) {
                importValidLink(properties);
            }
        } catch (NodeFactoryException | IOException e) {
            throw new StudyImporterException("failed to import: " + properties, e);
        }
    }

    private boolean validLink(Map<String, String> link) {
        return validLink(link, getLogger());
    }

    public static boolean validLink(Map<String, String> link, ImportLogger logger) {
        Predicate<Map<String, String>> hasSourceTaxon = (Map<String, String> l) -> {
            String sourceTaxonName = l.get(SOURCE_TAXON_NAME);
            String sourceTaxonId = l.get(SOURCE_TAXON_ID);
            boolean isValid = StringUtils.isNotBlank(sourceTaxonName) || StringUtils.isNotBlank(sourceTaxonId);
            if (!isValid) {
                logger.warn(null, "no source taxon info found in [" + l + "]");
            }
            return isValid;
        };

        Predicate<Map<String, String>> hasTargetTaxon = (Map<String, String> l) -> {
            String targetTaxonName = l.get(TARGET_TAXON_NAME);
            String targetTaxonId = l.get(TARGET_TAXON_ID);

            boolean isValid = StringUtils.isNotBlank(targetTaxonName) || StringUtils.isNotBlank(targetTaxonId);
            if (!isValid) {
                logger.warn(null, "no target taxon info found in [" + l + "]");
            }
            return isValid;
        };

        Predicate<Map<String, String>> hasInteractionType = createInteractionTypePredicate(logger);

        Predicate<Map<String, String>> hasReferenceId = createReferencePredicate(logger);

        return hasSourceTaxon
                .and(hasTargetTaxon)
                .and(hasInteractionType)
                .and(hasReferenceId)
                .test(link);
    }

    static Predicate<Map<String, String>> createReferencePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            boolean isValid = StringUtils.isNotBlank(l.get(REFERENCE_ID));
            if (!isValid && logger != null) {
                logger.warn(null, "missing [" + REFERENCE_ID + "]");
            }
            return isValid;
        };
    }

    static Predicate<Map<String, String>> createInteractionTypePredicate(ImportLogger logger) {
        return (Map<String, String> l) -> {
            String interactionTypeId = l.get(INTERACTION_TYPE_ID);
            boolean hasValidId = false;
            if (StringUtils.isBlank(interactionTypeId) && logger != null) {
                logger.warn(null, "missing [" + INTERACTION_TYPE_ID + "]");
            } else {
                hasValidId = InteractType.typeOf(interactionTypeId) != null;
                if (!hasValidId && logger != null) {
                    logger.warn(null, "found unsupported interactionTypeId [" + interactionTypeId + "]");
                }
            }
            return hasValidId;
        };
    }

    private void importValidLink(Map<String, String> link) throws StudyImporterException, IOException {
        Study study = nodeFactory.getOrCreateStudy(studyFromLink(link));

        Specimen source = createSpecimen(link, study, SOURCE_TAXON_NAME, SOURCE_TAXON_ID, SOURCE_BODY_PART_NAME, SOURCE_BODY_PART_ID, SOURCE_LIFE_STAGE_NAME, SOURCE_LIFE_STAGE_ID);
        setExternalIdNotBlank(link, SOURCE_OCCURRENCE_ID, source);
        Specimen target = createSpecimen(link, study, TARGET_TAXON_NAME, TARGET_TAXON_ID, TARGET_BODY_PART_NAME, TARGET_BODY_PART_ID, TARGET_LIFE_STAGE_NAME, TARGET_LIFE_STAGE_ID);
        setExternalIdNotBlank(link, TARGET_OCCURRENCE_ID, target);


        String interactionTypeId = link.get(INTERACTION_TYPE_ID);
        InteractType type = InteractType.typeOf(interactionTypeId);

        source.interactsWith(target, type, getOrCreateLocation(study, link));
    }

    private void setExternalIdNotBlank(Map<String, String> link, String sourceOccurrenceId, Specimen source1) {
        String s = link.get(sourceOccurrenceId);
        if (StringUtils.isNotBlank(s)) {
            source1.setExternalId(s);
        }
    }

    private Specimen createSpecimen(Map<String, String> link, Study study, String taxonNameLabel, String taxonIdLabel, String bodyPartName, String bodyPartId, String lifeStageName, String lifeStageId) throws StudyImporterException {
        String sourceTaxonName = link.get(taxonNameLabel);
        String sourceTaxonId = link.get(taxonIdLabel);
        String argumentTypeId = link.get(ARGUMENT_TYPE_ID);
        RelTypes[] argumentType = refutes(argumentTypeId)
                ? new RelTypes[]{RelTypes.REFUTES}
                : new RelTypes[]{RelTypes.COLLECTED, RelTypes.SUPPORTS};

        Specimen source = nodeFactory.createSpecimen(study, new TaxonImpl(sourceTaxonName, sourceTaxonId), argumentType);
        setBasisOfRecordIfAvailable(link, source);
        setDateTimeIfAvailable(link, source);
        setBodyPartIfAvailable(link, source, bodyPartName, bodyPartId);
        setLifeStageIfAvailable(link, source, lifeStageName, lifeStageId);
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

    private StudyImpl studyFromLink(Map<String, String> link) {
        String referenceCitation = link.get(REFERENCE_CITATION);
        DOI doi = null;
        String doiString = link.get(REFERENCE_DOI);
        try {
            doi = StringUtils.isBlank(doiString) ? null : DOI.create(doiString);
        } catch (MalformedDOIException e) {
            if (logger != null) {
                logger.warn(null, "found malformed doi [" + doiString + "]");
            }
        }
        StudyImpl study1 = new StudyImpl(link.get(REFERENCE_ID),
                link.get(STUDY_SOURCE_CITATION),
                doi,
                referenceCitation);

        final String referenceUrl = link.get(REFERENCE_URL);
        if (StringUtils.isBlank(study1.getExternalId()) && StringUtils.isNotBlank(referenceUrl)) {
            study1.setExternalId(referenceUrl);
        }

        return study1;
    }

    private void setDateTimeIfAvailable(Map<String, String> link, Specimen target) throws StudyImporterException {
        final String eventDate = link.get(StudyImporterForMetaTable.EVENT_DATE);
        if (StringUtils.isNotBlank(eventDate)) {
            try {
                final DateTime dateTime = DateUtil
                        .parseDateUTC(applySymbiotaDateTimeFix(eventDate));
                nodeFactory.setUnixEpochProperty(target, dateTime.toDate());
            } catch (IllegalArgumentException ex) {
                getLogger().warn(null, "invalid date string [" + eventDate + "]");
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

    private Location getOrCreateLocation(Study study, Map<String, String> link) throws IOException, NodeFactoryException {
        LatLng centroid = null;
        String[] latitudes = {DECIMAL_LATITUDE, StudyImporterForMetaTable.LATITUDE};
        String latitude = getFirstValueForTerms(link, latitudes);

        String[] longitudes = {DECIMAL_LONGITUDE, StudyImporterForMetaTable.LONGITUDE};
        String longitude = getFirstValueForTerms(link, longitudes);

        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                centroid = LocationUtil.parseLatLng(latitude, longitude);
            } catch (InvalidLocationException e) {
                getLogger().warn(study, "found invalid location: [" + e.getMessage() + "]");
            }
        }
        String localityId = getFirstValueForTerms(link, LOCALITY_ID_TERMS);

        if (centroid == null) {
            if (StringUtils.isNotBlank(localityId)) {
                try {
                    centroid = getGeoNamesService().findLatLng(localityId);
                } catch (IOException ex) {
                    if (getLogger() != null) {
                        getLogger().warn(study, "failed to lookup [" + localityId + "] because of: [" + ex.getMessage() + "]");
                    }
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
