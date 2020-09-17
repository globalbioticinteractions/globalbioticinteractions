package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractTypeMapperFactoryImpl;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DatasetImporterForINaturalist extends NodeBasedImporter {
    private static final Log LOG = LogFactory.getLog(DatasetImporterForINaturalist.class);

    public static final String INATURALIST_URL = "https://www.inaturalist.org";


    private final Map<Long, String> unsupportedInteractionTypes = new TreeMap<>();
    public static final String PREFIX_OBSERVATION_FIELD = INATURALIST_URL + "/observation_fields/";

    public DatasetImporterForINaturalist(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    static Taxon parseTaxon(JsonNode taxonNode) {
        String name = null;
        String externalId = null;

        JsonNode nameNode = taxonNode.get("name");
        if (nameNode != null && !nameNode.isNull()) {
            name = nameNode.asText();
        }

        JsonNode idNode = taxonNode.get("id");
        if (null != idNode) {
            externalId = TaxonomyProvider.INATURALIST_TAXON.getIdPrefix() + idNode.asText();
        }

        return (name == null && externalId == null) ? null : new TaxonImpl(name, externalId);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        unsupportedInteractionTypes.clear();
        retrieveDataParseResults();
        if (unsupportedInteractionTypes.size() > 0) {
            StringBuilder unsupportedInteractions = new StringBuilder();
            for (Map.Entry<Long, String> entry : unsupportedInteractionTypes.entrySet()) {
                StringBuilder unsupportedInteraction = new StringBuilder();
                unsupportedInteraction.append("https://www.inaturalist.org/observations/")
                        .append(entry.getKey())
                        .append(",")
                        .append(entry.getValue());
                getLogger().warn(null, unsupportedInteraction.toString());
                unsupportedInteractions.append("\n").append(unsupportedInteraction);

            }
            String msg = "found unsupported iNaturalist taxon fields: " + unsupportedInteractions.toString();
            throw new StudyImporterException(msg);
        }
    }

    protected String getSourceString() {
        String description = "http://iNaturalist.org is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ";
        return description + CitationUtil.createLastAccessedString(INATURALIST_URL);
    }

    private void retrieveDataParseResults() throws StudyImporterException {
        TermLookupService termLookupService;
        try {
            termLookupService = InteractTypeMapperFactoryImpl.getTermLookupService(getDataset(),
                    "observation_field_id",
                    "observation_field_id",
                    "observation_field_name",
                    "interaction_type_id",
                    URI.create("interaction_types_ignored.csv"),
                    URI.create("interaction_types.csv"));
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to find interaction term mapping", e);
        }

        int previousResultCount = 0;
        int pageNumber = 1;
        do {
            URI uri = URI.create(INATURALIST_URL + "/observation_field_values.json?type=taxon&page=" + pageNumber + "&per_page=100&quality_grade=research");

            try (InputStream resource = getDataset().retrieve(uri)) {
                previousResultCount = parseJSON(resource,
                        termLookupService);
                pageNumber++;
            } catch (IOException | StudyImporterException e) {
                throw new StudyImporterException("failed to import iNaturalist at [" + uri + "]", e);
            }

        } while (previousResultCount > 0);
    }

    int parseJSON(InputStream retargetAsStream, TermLookupService termLookupService) throws StudyImporterException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array;
        try {
            array = mapper.readTree(retargetAsStream);
        } catch (IOException e) {
            throw new StudyImporterException("error parsing inaturalist json", e);
        }
        if (!array.isArray()) {
            throw new StudyImporterException("expected json array, but found object");
        }
        for (int i = 0; i < array.size(); i++) {
            try {
                parseSingleInteractions(array.get(i), termLookupService);
            } catch (NodeFactoryException | IOException e) {
                throw new StudyImporterException("failed to parse inaturalist interactions", e);
            }
        }
        return array.size();
    }

    @Override
    public void setFilter(ImportFilter importFilter) {

    }

    private void parseSingleInteractions(JsonNode jsonNode, TermLookupService termLookupService) throws StudyImporterException, IOException {

        Taxon targetTaxon = null;
        Taxon sourceTaxon = null;
        if (jsonNode.has("taxon") && jsonNode.has("observation")) {
            targetTaxon = parseTaxon(jsonNode.get("taxon"));
            JsonNode observation = jsonNode.get("observation");
            if (jsonNode.has("taxon")) {
                sourceTaxon = parseTaxon(observation.get("taxon"));
            }
        }
        long observationId = jsonNode.get("observation_id").getLongValue();
        if (targetTaxon == null) {
            LOG.debug("skipping interaction with missing target taxon name for observation [" + observationId + "]");
        } else if (sourceTaxon == null) {
            LOG.warn("cannot create interaction with missing source taxon name for observation with id [" + observationId + "]");
        } else {
            JsonNode observationField = jsonNode.get("observation_field");
            String interactionDataType = observationField.get("datatype").getTextValue();
            String interactionTypeName = observationField.get("name").getTextValue();
            int interactionTypeId = observationField.get("id").getIntValue();
            String interactionTypeIdURI = "https://www.inaturalist.org/observation_fields/" + interactionTypeId;

            List<Term> mappedTerms;
            try {
                mappedTerms = mapInteractionType(new TermImpl(interactionTypeIdURI, interactionTypeName), termLookupService);
            } catch (TermLookupServiceException e) {
                throw new StudyImporterException("failed to map [" + interactionTypeIdURI + "] with label [" + interactionTypeName + "]");
            }

            if (mappedTerms == null || mappedTerms.isEmpty()) {
                LOG.debug("ignoring taxon observation field type [" + interactionTypeName + "] with id [" + interactionTypeId + "] for observation with id [" + observationId + "]");
            } else {
                for (Term mappedTerm : mappedTerms) {
                    InteractType interactType = InteractType.typeOf(mappedTerm.getId());
                    if (interactType == null) {
                        unsupportedInteractionTypes.put(observationId, interactionTypeName + "," + interactionTypeIdURI);
                        LOG.debug("no interaction type associated with observation field type [" + interactionTypeName + "] with id [" + interactionTypeId + "] for observation with id [" + observationId + "]");
                    } else {
                        handleObservation(jsonNode, targetTaxon, observationId, interactionDataType, interactType, interactionTypeName, sourceTaxon);
                    }
                }
            }
        }

    }

    private List<Term> mapInteractionType(TermImpl term, TermLookupService termLookupService) throws TermLookupServiceException {
        return termLookupService.lookupTermByName(term.getId());
    }

    private void handleObservation(JsonNode jsonNode, Taxon targetTaxon, long observationId, String interactionDataType, InteractType interactionTypeId, String interactionTypeName, Taxon sourceTaxon) throws StudyImporterException, NodeFactoryException {
        JsonNode observation = jsonNode.get("observation");
        importInteraction(targetTaxon, observationId, interactionDataType, interactionTypeId, observation, sourceTaxon, interactionTypeName);
    }

    private void importInteraction(Taxon targetTaxon, long observationId, String interactionDataType, InteractType interactionTypeId, JsonNode observation, Taxon sourceTaxon, String interactionTypeName) throws StudyImporterException, NodeFactoryException {
        Date observationDate = getObservationDate(observation);

        StringBuilder citation = buildCitation(observation, interactionTypeName, targetTaxon.getName(), sourceTaxon.getName(), observationDate);
        String url = ExternalIdUtil.urlForExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);
        citation.append(CitationUtil.createLastAccessedString(url));

        StudyImpl study1 = new StudyImpl(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId, getSourceString(), null, citation.toString());
        study1.setExternalId(url);

        Study study = getNodeFactory().getOrCreateStudy(study1);
        createAssociation(observationId, interactionDataType, interactionTypeId, observation, targetTaxon, sourceTaxon, study, observationDate);
    }

    private StringBuilder buildCitation(JsonNode observationNode, String interactionType, String targetTaxonName, String sourceTaxonName, Date observationDate) {
        StringBuilder citation = new StringBuilder();
        if (observationNode.has("user")) {
            JsonNode userNode = observationNode.get("user");
            String user = userNode.has("name") ? userNode.get("name").getTextValue() : "";
            String login = userNode.has("login") ? userNode.get("login").getTextValue() : "";
            citation.append(StringUtils.isBlank(user) ? login : user);
            citation.append(". ");
        }
        if (observationDate != null) {
            citation.append(DateUtil.printYear(observationDate));
            citation.append(". ");
        }
        citation.append(sourceTaxonName);
        citation.append(" ");
        citation.append(StringUtils.lowerCase(interactionType));
        citation.append(" ");
        citation.append(targetTaxonName);
        citation.append(". iNaturalist.org. ");
        return citation;
    }

    private Date getObservationDate(JsonNode observation) {
        DateTime dateTime = null;
        String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
        timeObservedAtUtc = timeObservedAtUtc == null ? observation.get("observed_on").getTextValue() : timeObservedAtUtc;
        if (timeObservedAtUtc != null) {
            dateTime = parseUTCDateTime(timeObservedAtUtc);
        }
        return dateTime == null ? null : dateTime.toDate();
    }

    private Specimen createAssociation(long observationId, String interactionDataType, InteractType interactType, JsonNode observation, Taxon targetTaxon, Taxon sourceTaxonName, Study study, Date observationDate) throws StudyImporterException, NodeFactoryException {
        Specimen sourceSpecimen = getSourceSpecimen(observationId, interactionDataType, sourceTaxonName, study);
        setBasisOfRecord(sourceSpecimen);
        Specimen targetSpecimen = getNodeFactory().createSpecimen(study, targetTaxon);
        setBasisOfRecord(targetSpecimen);

        setCollectionDate(sourceSpecimen, targetSpecimen, observationDate);
        setCollectionDate(sourceSpecimen, sourceSpecimen, observationDate);

        Location location = parseLocation(observation);
        sourceSpecimen.caughtIn(location);
        targetSpecimen.caughtIn(location);
        sourceSpecimen.interactsWith(targetSpecimen, interactType);

        return sourceSpecimen;
    }

    private void setBasisOfRecord(Specimen sourceSpecimen) throws NodeFactoryException {
        sourceSpecimen.setBasisOfRecord(getNodeFactory().getOrCreateBasisOfRecord("http://rs.tdwg.org/dwc/dwctype/HumanObservation", "HumanObservation"));
    }

    private Location parseLocation(JsonNode observation) throws NodeFactoryException {
        Location location = null;
        String latitudeString = observation.get("latitude").getTextValue();
        String longitudeString = observation.get("longitude").getTextValue();
        if (latitudeString != null && longitudeString != null) {
            double latitude = Double.parseDouble(latitudeString);
            double longitude = Double.parseDouble(longitudeString);
            location = getNodeFactory().getOrCreateLocation(new LocationImpl(latitude, longitude, null, null));

        }
        return location;
    }

    private void setCollectionDate(Specimen sourceSpecimen, Specimen targetSpecimen, Date observationDate) throws NodeFactoryException {
        getNodeFactory().setUnixEpochProperty(sourceSpecimen, observationDate);
        getNodeFactory().setUnixEpochProperty(targetSpecimen, observationDate);
    }

    private Specimen getSourceSpecimen(long observationId, String interactionDataType, Taxon sourceTaxon, Study study) throws StudyImporterException, NodeFactoryException {
        if (!"taxon".equals(interactionDataType)) {
            throw new StudyImporterException("expected [taxon] as observation_type datatype, but found [" + interactionDataType + "]");
        }
        Specimen sourceSpecimen = getNodeFactory().createSpecimen(study, sourceTaxon);
        sourceSpecimen.setExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);

        return sourceSpecimen;
    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(timeObservedAtUtc);
    }

}
