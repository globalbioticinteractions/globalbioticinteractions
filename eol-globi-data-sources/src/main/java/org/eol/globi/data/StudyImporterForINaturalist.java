package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForINaturalist extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForINaturalist.class);

    public static final String INATURALIST_URL = "http://inaturalist.org";

    private static final Map<String, InteractType> TYPE_MAPPING = new HashMap<String, InteractType>() {{
        put("Eating", InteractType.ATE);
        put("Feeding on:", InteractType.ATE);
        put("Feeding on", InteractType.ATE);
        put("Mariposas que alimenta", InteractType.ATE);
        put("With the prey", InteractType.ATE);
        put("Interaction: Preyed upon", InteractType.PREYS_UPON);
        put("Interaction: Ate fruit of", InteractType.ATE);
        put("Interaction: Herbivore of", InteractType.ATE);
        put("Interaction: Infected by", InteractType.PATHOGEN_OF);
        put("Predating", InteractType.PREYS_UPON);
        put("Depredando", InteractType.PREYS_UPON);
        put("Alimentándose", InteractType.ATE);
        put("Food Source", InteractType.ATE);
        put("Interaction: Fruit eaten by", InteractType.ATE);
        put("Host", InteractType.HAS_HOST);
        put("Host plant NZ", InteractType.HAS_HOST);
        put("host species with names lookup", InteractType.HAS_HOST);
        put("parasitic fungus on hoast with names lookup", InteractType.HAS_PARASITE);
        put("Hospedero", InteractType.HAS_PARASITE);
        put("host to parasitic fungus", InteractType.PARASITE_OF);
        put("Host animal", InteractType.HAS_HOST);
        put("Interaction: Visited flower of", InteractType.VISITS_FLOWERS_OF);
        put("Flower species", InteractType.POLLINATES);
        put("Euphorbia pulcherrima", InteractType.POLLINATES);
        put("Perching on", InteractType.PERCHING_ON);
        put("Pollinating", InteractType.POLLINATES);
        put("Other Species in Group", InteractType.INTERACTS_WITH);
        put("associated species NZ", InteractType.INTERACTS_WITH);
        put("Associated With", InteractType.INTERACTS_WITH);
        put("Milkweed species", InteractType.INTERACTS_WITH);
        put("second associated species", InteractType.INTERACTS_WITH);
        put("associated species alien to NZ", InteractType.INTERACTS_WITH);
        put("Associated species with names lookup", InteractType.INTERACTS_WITH);
        put("2nd associated organism with names lookup", InteractType.INTERACTS_WITH);
        put("Other other species in group", InteractType.INTERACTS_WITH);
        put("Pollinating", InteractType.POLLINATES);
        put("Butterfly & Moth Host Plant", InteractType.HAS_HOST);
        put("Butterfly & Moth Nectar Plant", InteractType.HAS_HOST);
        put("honey bee food plant", InteractType.HAS_HOST);
        put("Drinking nectar from", InteractType.HAS_HOST);
        put("Gall Inducer", InteractType.INTERACTS_WITH);
        put("Forms gall on", InteractType.INTERACTS_WITH);
        put("Insect Nectar Plant", InteractType.INTERACTS_WITH);
        put("specified substrate of fungus", InteractType.INTERACTS_WITH);
        put("Insect Host Plant", InteractType.HAS_HOST);
        put("Hunting", InteractType.PREYS_UPON);
        put("Predated by", InteractType.PREYED_UPON_BY);
        put("Is Host For", InteractType.HOST_OF);
        put("Being parasitized by", InteractType.HAS_PARASITE);
        put("Parasitando a", InteractType.PARASITE_OF);
        put("Interaction: Nested in", InteractType.INTERACTS_WITH);
        put("Interaction: Nested in", InteractType.INTERACTS_WITH);
        put("Interaction: Decomposer of", InteractType.INTERACTS_WITH);
        put("Interaction: Parasite/parasitoid of", InteractType.PARASITE_OF);
        put("Interaction: Flower visited by", InteractType.FLOWERS_VISITED_BY);
        put("Interaction: Preyed upon by", InteractType.PREYED_UPON_BY);
    }};

    private static final Map<String, InteractType> INVERSE_TYPE_MAPPING = new HashMap<String, InteractType>() {{
        put("Eaten by", InteractType.ATE);
        put("Hunted by", InteractType.PREYS_UPON);
        put("Associated parasitic / pathogenic organism with names lookup", InteractType.PARASITE_OF);
        put("first nectar or pollen feeding insect", InteractType.POLLINATES);
        put("second nectar or pollen feeding insect", InteractType.POLLINATES);
    }};
    public static final List<String> IGNORED_INTERACTION_TYPES = new ArrayList<String>() {{
        // see https://github.com/jhpoelen/eol-globi-data/issues/56
        add("Syntopic");
        add("Syntop");
        add("Target species");
        add("Iconic taxon name");
        add("Tree species");
        add("associated fern plant, pteridophyte");
        add("Order");
        add("Class");
        add("Phylum");
        add("Unidentified");
        add("Animal observed");
    }};
    private final Map<Long, String> unsupportedInteractionTypes = new TreeMap<Long, String>();

    public StudyImporterForINaturalist(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        unsupportedInteractionTypes.clear();
        getSourceString();
        retrieveDataParseResults();
        if (unsupportedInteractionTypes.size() > 0) {
            StringBuilder unsupportedInteractions = new StringBuilder();
            for (Map.Entry<Long, String> entry : unsupportedInteractionTypes.entrySet()) {
                unsupportedInteractions.append("([")
                        .append(entry.getKey())
                        .append("], [")
                        .append(entry.getValue())
                        .append("]) ");
            }
            String msg = "found unsupported (observationId, interactionType) pairs: " + unsupportedInteractions.toString();
            throw new StudyImporterException(msg);
        }
        return null;
    }

    protected String getSourceString() {
        String description = "http://iNaturalist.org is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ";
        return description + ReferenceUtil.createLastAccessedString(INATURALIST_URL);
    }

    private int retrieveDataParseResults() throws StudyImporterException {
        int totalInteractions = 0;
        int previousResultCount = 0;
        int pageNumber = 1;
        do {
            String uri = "http://www.inaturalist.org/observation_field_values.json?type=taxon&page=" + pageNumber + "&per_page=100&license=any&quality_grade=research";
            HttpGet httpGet = new HttpGet(uri);
            try {
                httpGet.addHeader("accept", "application/json");
                HttpResponse response = HttpUtil.getHttpClient().execute(httpGet);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new StudyImporterException("failed to execute query to [ " + uri + "]: status code [" + response.getStatusLine().getStatusCode() + "]");
                }
                previousResultCount = parseJSON(response.getEntity().getContent());
                pageNumber++;
                totalInteractions += previousResultCount;
            } catch (IOException e) {
                throw new StudyImporterException("failed to import iNaturalist", e);
            } finally {
                httpGet.releaseConnection();
            }

        } while (previousResultCount > 0);
        return totalInteractions;
    }

    protected int parseJSON(InputStream retargetAsStream) throws StudyImporterException {
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
                parseSingleInteractions(array.get(i));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to parse inaturalist interactions", e);
            }
        }
        return array.size();
    }

    @Override
    public void setFilter(ImportFilter importFilter) {

    }

    private void parseSingleInteractions(JsonNode jsonNode) throws NodeFactoryException, StudyImporterException {
        JsonNode targetTaxon = jsonNode.get("taxon");
        JsonNode targetTaxonNode = targetTaxon.get("name");
        long observationId = jsonNode.get("observation_id").getLongValue();
        if (targetTaxonNode == null) {
            LOG.warn("skipping interaction with missing target taxon name for observation [" + observationId + "]");
        } else {
            JsonNode observationField = jsonNode.get("observation_field");
            String interactionDataType = observationField.get("datatype").getTextValue();
            String interactionType = observationField.get("name").getTextValue();
            if (isIgnoredInteractionType(interactionType)) {
                LOG.warn("ignoring taxon observation field type [" + interactionType + "] for observation with id [" + observationId + "]");
            } else {
                handleObservation(jsonNode, targetTaxonNode, observationId, interactionDataType, interactionType);
            }
        }

    }

    private boolean isIgnoredInteractionType(String interactionType) {
        return StringUtils.isBlank(interactionType) || IGNORED_INTERACTION_TYPES.contains(interactionType);
    }


    private void handleObservation(JsonNode jsonNode, JsonNode targetTaxonNode, long observationId, String interactionDataType, String interactionType) throws StudyImporterException, NodeFactoryException {
        JsonNode observation = jsonNode.get("observation");

        JsonNode sourceTaxon = observation.get("taxon");
        if (sourceTaxon == null) {
            LOG.warn("cannot create interaction with missing source taxon name for observation with id [" + observation.get("id") + "]");
        } else {
            if (TYPE_MAPPING.containsKey(interactionType) || INVERSE_TYPE_MAPPING.containsKey(interactionType)) {
                importInteraction(targetTaxonNode, observationId, interactionDataType, interactionType, observation, sourceTaxon);
            } else {
                unsupportedInteractionTypes.put(observationId, interactionType);
            }
        }
    }

    private void importInteraction(JsonNode targetTaxonNode, long observationId, String interactionDataType, String interactionType, JsonNode observation, JsonNode sourceTaxon) throws StudyImporterException, NodeFactoryException {
        Study study = nodeFactory.getOrCreateStudy2(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId, getSourceString(), null);
        String targetTaxonName = targetTaxonNode.getTextValue();
        String sourceTaxonName = sourceTaxon.get("name").getTextValue();
        Date observationDate = getObservationDate(study, observationId, observation);

        if (TYPE_MAPPING.containsKey(interactionType)) {
            createAssociation(observationId, interactionDataType, interactionType, observation, targetTaxonName, sourceTaxonName, study, observationDate);
        } else if (INVERSE_TYPE_MAPPING.containsKey(interactionType)) {
            createInverseAssociation(observationId, interactionDataType, interactionType, observation, targetTaxonName, sourceTaxonName, study, observationDate);
        }
        StringBuilder citation = buildCitation(observation, interactionType, targetTaxonName, sourceTaxonName, observationDate);
        String url = ExternalIdUtil.urlForExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);
        citation.append(ReferenceUtil.createLastAccessedString(url));
        study.setCitationWithTx(citation.toString());
        study.setExternalId(url);
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
            SimpleDateFormat format = new SimpleDateFormat("yyyy");
            citation.append(format.format(observationDate));
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

    private Date getObservationDate(Study study, long observationId, JsonNode observation) {
        DateTime dateTime = null;
        String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
        timeObservedAtUtc = timeObservedAtUtc == null ? observation.get("observed_on").getTextValue() : timeObservedAtUtc;
        if (timeObservedAtUtc == null) {
            getLogger().warn(study, "failed to retrieve observation time for observation [" + observationId + "]");
        } else {
            dateTime = parseUTCDateTime(timeObservedAtUtc);
        }
        return dateTime == null ? null : dateTime.toDate();
    }

    private Specimen createInverseAssociation(long observationId, String interactionDataType, String interactionType, JsonNode observation, String targetTaxonName, String sourceTaxonName, Study study, Date observationDate) throws StudyImporterException, NodeFactoryException {
        Specimen sourceSpecimen = getSourceSpecimen(observationId, interactionDataType, sourceTaxonName, study);
        Specimen targetSpecimen = nodeFactory.createSpecimen(study, targetTaxonName);
        targetSpecimen.interactsWith(sourceSpecimen, INVERSE_TYPE_MAPPING.get(interactionType));
        setCollectionDate(sourceSpecimen, targetSpecimen, observationDate);
        return targetSpecimen;
    }

    private Specimen createAssociation(long observationId, String interactionDataType, String interactionType, JsonNode observation, String targetTaxonName, String sourceTaxonName, Study study, Date observationDate) throws StudyImporterException, NodeFactoryException {
        Specimen sourceSpecimen = getSourceSpecimen(observationId, interactionDataType, sourceTaxonName, study);
        setBasisOfRecord(sourceSpecimen);
        Specimen targetSpecimen = nodeFactory.createSpecimen(study, targetTaxonName);
        setBasisOfRecord(targetSpecimen);

        sourceSpecimen.interactsWith(targetSpecimen, TYPE_MAPPING.get(interactionType));
        setCollectionDate(sourceSpecimen, targetSpecimen, observationDate);
        setCollectionDate(sourceSpecimen, sourceSpecimen, observationDate);

        Location location = parseLocation(observation);
        sourceSpecimen.caughtIn(location);
        targetSpecimen.caughtIn(location);

        return sourceSpecimen;
    }

    private void setBasisOfRecord(Specimen sourceSpecimen) throws NodeFactoryException {
        sourceSpecimen.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord("http://rs.tdwg.org/dwc/dwctype/HumanObservation", "HumanObservation"));
    }

    private Location parseLocation(JsonNode observation) throws NodeFactoryException {
        Location location = null;
        String latitudeString = observation.get("latitude").getTextValue();
        String longitudeString = observation.get("longitude").getTextValue();
        if (latitudeString != null && longitudeString != null) {
            double latitude = Double.parseDouble(latitudeString);
            double longitude = Double.parseDouble(longitudeString);
            location = nodeFactory.getOrCreateLocation(latitude, longitude, null);

        }
        return location;
    }

    private void setCollectionDate(Specimen sourceSpecimen, Specimen targetSpecimen, Date observationDate) throws NodeFactoryException {
        nodeFactory.setUnixEpochProperty(sourceSpecimen, observationDate);
        nodeFactory.setUnixEpochProperty(targetSpecimen, observationDate);
    }

    private Specimen getSourceSpecimen(long observationId, String interactionDataType, String sourceTaxonName, Study study) throws StudyImporterException, NodeFactoryException {
        if (!"taxon".equals(interactionDataType)) {
            throw new StudyImporterException("expected [taxon] as observation_type datatype, but found [" + interactionDataType + "]");
        }
        Specimen sourceSpecimen = nodeFactory.createSpecimen(study, sourceTaxonName);
        sourceSpecimen.setExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);

        return sourceSpecimen;
    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }

    @Override
    public boolean shouldCrossCheckReference() {
        return false;
    }
}
