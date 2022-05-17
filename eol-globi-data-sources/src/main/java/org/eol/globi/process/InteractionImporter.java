package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.DatasetImporterForMetaTable;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.LocationUtil;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
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
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.ARGUMENT_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.BASIS_OF_RECORD_ID;
import static org.eol.globi.data.DatasetImporterForTSV.BASIS_OF_RECORD_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
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

public class InteractionImporter implements InteractionListener {

    private static final String[] LOCALITY_ID_TERMS = {LOCALITY_ID, DwcTerm.locationID.normQName};
    private static final String[] LOCALITY_NAME_TERMS = {LOCALITY_NAME, DwcTerm.locality.normQName, DwcTerm.verbatimLocality.normQName};
    private final ImportLogger logger;
    private final NodeFactory nodeFactory;
    private final GeoNamesService geoNamesService;


    public InteractionImporter(NodeFactory factory, ImportLogger logger, GeoNamesService geonamesService) {
        this.logger = logger;
        this.nodeFactory = factory;
        this.geoNamesService = geonamesService;
    }

    public InteractionImporter(NodeFactory factory, GeoNamesService geonamesService, ImportLogger logger) {
        this.logger = logger;
        this.nodeFactory = factory;
        this.geoNamesService = geonamesService;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        importInteraction(interaction);
    }


    private void importInteraction(Map<String, String> interaction) throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy(studyOf(interaction));

        Specimen source = createSpecimen(
                interaction,
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

        setExternalIdNotBlank(interaction, SOURCE_OCCURRENCE_ID, source);
        setPropertyIfAvailable(interaction, source, SOURCE_OCCURRENCE_ID, OCCURRENCE_ID);
        setPropertyIfAvailable(interaction, source, SOURCE_CATALOG_NUMBER, CATALOG_NUMBER);
        setPropertyIfAvailable(interaction, source, SOURCE_COLLECTION_CODE, COLLECTION_CODE);
        setPropertyIfAvailable(interaction, source, SOURCE_COLLECTION_ID, COLLECTION_ID);
        setPropertyIfAvailable(interaction, source, SOURCE_INSTITUTION_CODE, INSTITUTION_CODE);

        Specimen target = createSpecimen(
                interaction,
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

        setExternalIdNotBlank(interaction, TARGET_OCCURRENCE_ID, target);
        setPropertyIfAvailable(interaction, target, TARGET_OCCURRENCE_ID, OCCURRENCE_ID);
        setPropertyIfAvailable(interaction, target, TARGET_CATALOG_NUMBER, CATALOG_NUMBER);
        setPropertyIfAvailable(interaction, target, TARGET_COLLECTION_CODE, COLLECTION_CODE);
        setPropertyIfAvailable(interaction, target, TARGET_COLLECTION_ID, COLLECTION_ID);
        setPropertyIfAvailable(interaction, target, TARGET_INSTITUTION_CODE, INSTITUTION_CODE);


        String interactionTypeId = interaction.get(INTERACTION_TYPE_ID);
        InteractType type = InteractType.typeOf(interactionTypeId);

        source.interactsWith(target, type, getOrCreateLocation(interaction));
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
                                    String sexId,
                                    String taxonRankLabel,
                                    String taxonPathIdsLabel) throws StudyImporterException {
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

        Specimen specimen = nodeFactory.createSpecimen(study, taxon, argumentType);
        setBasisOfRecordIfAvailable(link, specimen);
        setDateTimeIfAvailable(link, specimen);
        setBodyPartIfAvailable(link, specimen, bodyPartName, bodyPartId);
        setLifeStageIfAvailable(link, specimen, lifeStageName, lifeStageId);
        setSexIfAvailable(link, specimen, sexLabel, sexId);
        return specimen;
    }

    private boolean refutes(String argumentTypeId) {
        return StringUtils.equalsIgnoreCase(argumentTypeId, PropertyAndValueDictionary.REFUTES);
    }

    private void setLifeStageIfAvailable(Map<String, String> link, Specimen specimen, String name, String id) {
        final String lifeStageName = link.get(name);
        final String lifeStageId = link.get(id);
        if (StringUtils.isNotBlank(lifeStageName) || StringUtils.isNotBlank(lifeStageId)) {
            specimen.setLifeStage(new TermImpl(lifeStageId, lifeStageName));
        }
    }

    private void setSexIfAvailable(Map<String, String> link, Specimen specimen, String name, String id) {
        final String sexName = link.get(name);
        final String sexId = link.get(id);
        if (StringUtils.isNotBlank(sexName) || StringUtils.isNotBlank(sexId)) {
            specimen.setSex(new TermImpl(sexId, sexName));
        }
    }

    private StudyImpl studyOf(Map<String, String> l) {
        String referenceCitation = l.get(REFERENCE_CITATION);
        DOI doi = null;
        String doiString = l.get(REFERENCE_DOI);
        try {
            doi = StringUtils.isBlank(doiString) ? null : DOI.create(doiString);
        } catch (MalformedDOIException e) {
            LogUtil.logWarningIfPossible(l, "found malformed doi [" + doiString + "]", logger);
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

                String msg = DateUtil.validateDate(eventDate, dateTime);

                if (StringUtils.isNoneBlank(msg)) {
                    logWarningIfPossible(link, msg);
                }

                nodeFactory.setUnixEpochProperty(target, dateTime.toDate());
            } catch (IllegalArgumentException ex) {
                logWarningIfPossible(link, "invalid date string [" + eventDate + "]");
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to set time for [" + eventDate + "]", e);
            }

        }

    }


    private String applySymbiotaDateTimeFix(String eventDate) {
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

    private Location getOrCreateLocation(Map<String, String> interaction) throws NodeFactoryException {
        LatLng centroid = null;
        String[] latitudes = {DECIMAL_LATITUDE, DatasetImporterForMetaTable.LATITUDE};
        String latitude = getFirstValueForTerms(interaction, latitudes);

        String[] longitudes = {DECIMAL_LONGITUDE, DatasetImporterForMetaTable.LONGITUDE};
        String longitude = getFirstValueForTerms(interaction, longitudes);

        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                centroid = LocationUtil.parseLatLng(latitude, longitude);
            } catch (InvalidLocationException e) {
                logWarningIfPossible(interaction, "found invalid location: [" + e.getMessage() + "]");
            }
        }
        String localityId = getFirstValueForTerms(interaction, LOCALITY_ID_TERMS);

        if (centroid == null) {
            if (StringUtils.isNotBlank(localityId)) {
                try {
                    centroid = geoNamesService.findLatLng(localityId);
                } catch (IOException ex) {
                    String message = "failed to lookup [" + localityId + "] because of: [" + ex.getMessage() + "]";
                    logWarningIfPossible(interaction, message);
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
            String localityName = getFirstValueForTerms(interaction, LOCALITY_NAME_TERMS);
            if (StringUtils.isNotBlank(localityName)) {
                location.setLocality(localityName);
            }
        }
        return location == null ? null : nodeFactory.getOrCreateLocation(location);
    }

    private void logWarningIfPossible(Map<String, String> link, String message) {
        LogUtil.logWarningIfPossible(link, message, logger);
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

}
