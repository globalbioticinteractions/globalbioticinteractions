package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
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

public class StudyImporterForINaturalist extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForINaturalist.class);

    public static final String INATURALIST_URL = "http://inaturalist.org";

    private static final Map<String, InteractType> TYPE_MAPPING = new HashMap<String, InteractType>() {{
        put("Eating", InteractType.ATE);
        put("With the prey", InteractType.ATE);
        put("Alimentándose", InteractType.ATE);
        put("Host", InteractType.HAS_HOST);
        put("Flower species", InteractType.POLLINATES);
        put("Perching on", InteractType.PERCHING_ON);
        put("Pollinating", InteractType.POLLINATES);
        put("Other Species in Group", InteractType.INTERACTS_WITH);
        put("Other other species in group", InteractType.INTERACTS_WITH);
        put("Pollinating", InteractType.POLLINATES);
        put("Butterfly & Moth Host Plant", InteractType.HAS_HOST);
        put("Butterfly & Moth Nectar Plant", InteractType.HAS_HOST);
        put("Drinking nectar from", InteractType.HAS_HOST);
        put("Gall Inducer", InteractType.INTERACTS_WITH);
        put("Forms gall on", InteractType.INTERACTS_WITH);
        put("Insect Nectar Plant", InteractType.INTERACTS_WITH);
        put("Insect Host Plant", InteractType.HAS_HOST);
    }};

    private static final Map<String, InteractType> INVERSE_TYPE_MAPPING = new HashMap<String, InteractType>() {{
        put("Eaten by", InteractType.ATE);
    }};
    public static final int MAX_ATTEMPTS = 3;
    public static final List<String> IGNORED_INTERACTION_TYPES = new ArrayList<String>() {{
        // see https://github.com/jhpoelen/eol-globi-data/issues/56
        add("Syntopic");
        add("Associated species with names lookup");
        add("Target species");
        add("Tree species");
    }};

    public StudyImporterForINaturalist(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        getSourceString();
        retrieveDataParseResults();
        return null;
    }

    protected String getSourceString() {
        String description = "http://iNaturalist.org is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ";
        return description + ReferenceUtil.createLastAccessedString(INATURALIST_URL);
    }

    private int retrieveDataParseResults() throws StudyImporterException {
        int totalInteractions = 0;
        HttpClient defaultHttpClient = HttpUtil.createHttpClient();
        try {
            int previousResultCount = 0;
            int pageNumber = 1;
            int attempt = 0;
            do {
                String uri = "http://www.inaturalist.org/observation_field_values.json?type=taxon&page=" + pageNumber + "&per_page=100&license=any";
                HttpGet get = new HttpGet(uri);
                get.addHeader("accept", "application/json");
                try {
                    HttpResponse response = defaultHttpClient.execute(get);
                    if (response.getStatusLine().getStatusCode() != 200) {
                        throw new StudyImporterException("failed to execute query to [ " + uri + "]: status code [" + response.getStatusLine().getStatusCode() + "]");
                    }
                    attempt = 0;
                    previousResultCount = parseJSON(response.getEntity().getContent());
                    pageNumber++;
                    totalInteractions += previousResultCount;
                } catch (IOException e) {
                    String msg = "failed to execute query to [ " + uri + "]";
                    if (attempt < MAX_ATTEMPTS) {
                        LOG.warn(msg + " retry [" + attempt + "] of [" + MAX_ATTEMPTS + "]", e);
                    } else {
                        throw new StudyImporterException(msg, e);
                    }
                }

            } while (previousResultCount > 0);
        } finally {
            defaultHttpClient.getConnectionManager().shutdown();
        }
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
                createInteraction(jsonNode, targetTaxonNode, observationId, interactionDataType, interactionType);
            }
        }

    }

    private boolean isIgnoredInteractionType(String interactionType) {
        return StringUtils.isBlank(interactionType) || IGNORED_INTERACTION_TYPES.contains(interactionType);
    }


    private void createInteraction(JsonNode jsonNode, JsonNode targetTaxonNode, long observationId, String interactionDataType, String interactionType) throws StudyImporterException, NodeFactoryException {
        JsonNode observation = jsonNode.get("observation");

        JsonNode sourceTaxon = observation.get("taxon");
        if (sourceTaxon == null) {
            LOG.warn("cannot create interaction with missing source taxon name for observation with id [" + observation.get("id") + "]");
        } else {
            Study study = nodeFactory.getOrCreateStudy(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId, getSourceString(), null);
            Specimen specimen;
            String targetTaxonName = targetTaxonNode.getTextValue();
            String sourceTaxonName = sourceTaxon.get("name").getTextValue();
            if (TYPE_MAPPING.containsKey(interactionType)) {
                specimen = createAssociation(observationId, interactionDataType, interactionType, observation, targetTaxonName, sourceTaxonName);
            } else if (INVERSE_TYPE_MAPPING.containsKey(interactionType)) {
                specimen = createInverseAssociation(observationId, interactionDataType, interactionType, observation, targetTaxonName, sourceTaxonName);
            } else {
                throw new StudyImporterException("found unsupported interactionType [" + interactionType + "] for observation [" + observationId + "]");
            }
            Date observationDate = getObservationDate(study, observationId, observation);

            StringBuilder citation = buildCitation(observation, interactionType, targetTaxonName, sourceTaxonName, observationDate);
            String url = ExternalIdUtil.infoURLForExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);
            citation.append(ReferenceUtil.createLastAccessedString(url));
            study.setCitationWithTx(citation.toString());
            study.setExternalId(url);
            nodeFactory.setUnixEpochProperty(study.collected(specimen), observationDate);
        }
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

    private Specimen createInverseAssociation(long observationId, String interactionDataType, String interactionType, JsonNode observation, String targetTaxonName, String sourceTaxonName) throws StudyImporterException, NodeFactoryException {
        Specimen sourceSpecimen = getSourceSpecimen(observationId, interactionDataType, observation, sourceTaxonName);
        Specimen targetSpecimen = nodeFactory.createSpecimen(targetTaxonName);
        targetSpecimen.interactsWith(sourceSpecimen, INVERSE_TYPE_MAPPING.get(interactionType));
        return targetSpecimen;
    }

    private Specimen createAssociation(long observationId, String interactionDataType, String interactionType, JsonNode observation, String targetTaxonName, String sourceTaxonName) throws StudyImporterException, NodeFactoryException {
        Specimen sourceSpecimen = getSourceSpecimen(observationId, interactionDataType, observation, sourceTaxonName);
        Specimen targetSpecimen = nodeFactory.createSpecimen(targetTaxonName);
        sourceSpecimen.interactsWith(targetSpecimen, TYPE_MAPPING.get(interactionType));
        return sourceSpecimen;
    }

    private Specimen getSourceSpecimen(long observationId, String interactionDataType, JsonNode observation, String sourceTaxonName) throws StudyImporterException, NodeFactoryException {
        if (!"taxon".equals(interactionDataType)) {
            throw new StudyImporterException("expected [taxon] as observation_type datatype, but found [" + interactionDataType + "]");
        }
        Specimen sourceSpecimen = nodeFactory.createSpecimen(sourceTaxonName);
        sourceSpecimen.setExternalId(TaxonomyProvider.ID_PREFIX_INATURALIST + observationId);

        String latitudeString = observation.get("latitude").getTextValue();
        String longitudeString = observation.get("longitude").getTextValue();
        if (latitudeString != null && longitudeString != null) {
            double latitude = Double.parseDouble(latitudeString);
            double longitude = Double.parseDouble(longitudeString);
            sourceSpecimen.caughtIn(nodeFactory.getOrCreateLocation(latitude, longitude, null));
        }
        return sourceSpecimen;
    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
