package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.InvalidLocationException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.eol.globi.data.StudyImporterForTSV.ASSOCIATED_TAXA;
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
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.SOURCE_TAXON_NAME;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_BODY_PART_NAME;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_ID;
import static org.eol.globi.data.StudyImporterForTSV.TARGET_TAXON_NAME;

class InteractionListenerImpl implements InteractionListener {
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
            List<Map<String, String>> propertiesList = expandIfNeeded(properties);
            for (Map<String, String> expandedProperties : propertiesList) {
                if (properties != null && validLink(expandedProperties)) {
                    importValidLink(expandedProperties);
                }
            }
        } catch (NodeFactoryException | IOException e) {
            throw new StudyImporterException("failed to import: " + properties, e);
        }
    }

    private List<Map<String, String>> expandIfNeeded(Map<String, String> properties) {
        List<Map<String, String>> expandedList = Arrays.asList(properties);
        String associatedTaxa = properties.get(ASSOCIATED_TAXA);
        return StringUtils.isNotBlank(associatedTaxa)
                ? expand(properties, associatedTaxa)
                : expandedList;
    }

    private List<Map<String, String>> expand(Map<String, String> properties, String associatedTaxa) {
        List<Map<String, String>> expandedList;
        expandedList = new ArrayList<>();
        String[] associatedPairs = CSVTSVUtil.splitPipes(associatedTaxa);
        for (String associatedPair : associatedPairs) {
            String[] typeAndTarget = associatedPair.split(":");
            if (typeAndTarget.length > 1) {
                String typeName = typeAndTarget[0];
                String targetTaxon = typeAndTarget[1];
                HashMap<String, String> expanded = new HashMap<>(properties);
                expanded.put(TARGET_TAXON_NAME, StringUtils.trim(targetTaxon));
                expanded.put(INTERACTION_TYPE_NAME, StringUtils.trim(typeName));
                InteractType interactType = StudyImporterForMetaTable.generateInteractionType(expanded);
                if (interactType != null) {
                    expanded.put(INTERACTION_TYPE_NAME, interactType.getLabel());
                    expanded.put(INTERACTION_TYPE_ID, interactType.getIRI());
                } else {
                    getLogger().warn(studyFromLink(expanded), "unsupported interaction type for name [" + typeName + "]");
                }
                expandedList.add(expanded);
            }
        }
        return expandedList;
    }

    private boolean validLink(Map<String, String> link) {
        Predicate<Map<String, String>> hasSourceTaxon = (Map<String, String> l) -> {
            String sourceTaxonName = l.get(SOURCE_TAXON_NAME);
            String sourceTaxonId = l.get(SOURCE_TAXON_ID);
            boolean isValid = StringUtils.isNotBlank(sourceTaxonName) || StringUtils.isNotBlank(sourceTaxonId);
            if (!isValid) {
                getLogger().warn(null, "no source taxon info found in [" + l + "]");
            }
            return isValid;
        };

        Predicate<Map<String, String>> hasTargetTaxon = (Map<String, String> l) -> {
            String targetTaxonName = l.get(TARGET_TAXON_NAME);
            String targetTaxonId = l.get(TARGET_TAXON_ID);

            boolean isValid = StringUtils.isNotBlank(targetTaxonName) || StringUtils.isNotBlank(targetTaxonId);
            if (!isValid) {
                getLogger().warn(null, "no target taxon info found in [" + l + "]");
            }
            return isValid;
        };

        Predicate<Map<String, String>> hasInteractionType = (Map<String, String> l) -> {
            String interactionTypeId = l.get(INTERACTION_TYPE_ID);
            boolean isValid = InteractType.typeOf(interactionTypeId) != null;
            if (!isValid) {
                getLogger().warn(null, "found unsupported interactionTypeId [" + interactionTypeId + "]");
            }
            return isValid;
        };

        Predicate<Map<String, String>> hasReferenceId = (Map<String, String> l) -> StringUtils.isNotBlank(l.get(REFERENCE_ID));

        return hasSourceTaxon
                .and(hasTargetTaxon)
                .and(hasInteractionType)
                .and(hasReferenceId)
                .test(link);
    }

    private void importValidLink(Map<String, String> link) throws StudyImporterException, IOException {
        Study study = nodeFactory.getOrCreateStudy(studyFromLink(link));

        Specimen source = createSpecimen(link, study, SOURCE_TAXON_NAME, SOURCE_TAXON_ID, SOURCE_BODY_PART_NAME, SOURCE_BODY_PART_ID);
        Specimen target = createSpecimen(link, study, TARGET_TAXON_NAME, TARGET_TAXON_ID, TARGET_BODY_PART_NAME, TARGET_BODY_PART_ID);

        String interactionTypeId = link.get(INTERACTION_TYPE_ID);
        InteractType type = InteractType.typeOf(interactionTypeId);

        source.interactsWith(target, type, getOrCreateLocation(study, link));
    }

    private Specimen createSpecimen(Map<String, String> link, Study study, String taxonNameLabel, String taxonIdLabel, String bodyPartName, String bodyPartId) throws StudyImporterException {
        String sourceTaxonName = link.get(taxonNameLabel);
        String sourceTaxonId = link.get(taxonIdLabel);
        Specimen source = nodeFactory.createSpecimen(study, new TaxonImpl(sourceTaxonName, sourceTaxonId));
        setBasisOfRecordIfAvailable(link, source);
        setDateTimeIfAvailable(link, source);
        setBodyPartIfAvailable(link, source, bodyPartName, bodyPartId);
        return source;
    }

    private StudyImpl studyFromLink(Map<String, String> link) {
        String referenceCitation = link.get(REFERENCE_CITATION);
        StudyImpl study1 = new StudyImpl(link.get(REFERENCE_ID),
                link.get(STUDY_SOURCE_CITATION),
                link.get(REFERENCE_DOI),
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
                final DateTime dateTime = ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(eventDate);
                nodeFactory.setUnixEpochProperty(target, dateTime.toDate());
            } catch (IllegalArgumentException ex) {
                throw new StudyImporterException("invalid date string [" + eventDate + "]", ex);
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to set time for [" + eventDate + "]", e);
            }

        }

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
        String localityId = link.get(LOCALITY_ID);

        if (centroid == null) {
            if (StringUtils.isNotBlank(localityId)) {
                centroid = getGeoNamesService().findLatLng(localityId);
            }
        }

        LocationImpl location = null;
        if (centroid != null) {
            location = new LocationImpl(centroid.getLat(),
                    centroid.getLng(), null, null);
            if (StringUtils.isNotBlank(localityId)) {
                location.setLocalityId(localityId);
            }
            String localityName = link.get(LOCALITY_NAME);
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
