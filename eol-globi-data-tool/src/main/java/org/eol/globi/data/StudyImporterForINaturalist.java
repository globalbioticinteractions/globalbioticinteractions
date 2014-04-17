package org.eol.globi.data;

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
import org.eol.globi.util.HttpUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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

    public StudyImporterForINaturalist(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("iNaturalist", "", INATURALIST_URL, "", "http://iNaturalist.org is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ", "", INATURALIST_URL);
        study.setExternalId(INATURALIST_URL);
        retrieveDataParseResults(study);
        return study;
    }

    private int retrieveDataParseResults(Study study) throws StudyImporterException {
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
                    previousResultCount = parseJSON(response.getEntity().getContent(), study);
                    pageNumber++;
                    totalInteractions += previousResultCount;
                    getLogger().info(study, "importing [" + pageNumber + "] total [" + totalInteractions + "]");
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

    protected int parseJSON(InputStream retargetAsStream, Study study) throws StudyImporterException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = null;
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
                parseSingleInteractions(study, array.get(i));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to parse inaturalist interactions", e);
            }
        }
        return array.size();
    }

    @Override
    public void setFilter(ImportFilter importFilter) {

    }

    private void parseSingleInteractions(Study study, JsonNode jsonNode) throws NodeFactoryException, StudyImporterException {
        JsonNode targetTaxon = jsonNode.get("taxon");
        JsonNode targetTaxonNode = targetTaxon.get("name");
        long observationId = jsonNode.get("observation_id").getLongValue();
        if (targetTaxonNode == null) {
            getLogger().warn(study, "skipping interaction with missing target taxon name for observation [" + observationId + "]");
        } else {
            String targetTaxonName = targetTaxonNode.getTextValue();
            Specimen targetSpecimen = nodeFactory.createSpecimen(targetTaxonName);

            JsonNode observationField = jsonNode.get("observation_field");
            String interactionDataType = observationField.get("datatype").getTextValue();
            String interactionType = observationField.get("name").getTextValue();

            JsonNode observation = jsonNode.get("observation");

            JsonNode sourceTaxon = observation.get("taxon");
            if (sourceTaxon == null) {
                getLogger().warn(study, "cannot create interaction with missing source taxon name for observation with id [" + observation.get("id") + "]");
            } else {
                JsonNode sourceTaxonNameNode = sourceTaxon.get("name");
                String sourceTaxonName = sourceTaxonNameNode.getTextValue();
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

                Relationship collectedRel;

                if (TYPE_MAPPING.containsKey(interactionType)) {
                    sourceSpecimen.interactsWith(targetSpecimen, TYPE_MAPPING.get(interactionType));
                    collectedRel = study.collected(sourceSpecimen);
                } else if (INVERSE_TYPE_MAPPING.containsKey(interactionType)) {
                    targetSpecimen.interactsWith(sourceSpecimen, INVERSE_TYPE_MAPPING.get(interactionType));
                    collectedRel = study.collected(targetSpecimen);
                } else {
                    throw new StudyImporterException("found unsupported interactionType [" + interactionType + "] for observation [" + observationId + "]");
                }

                String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
                timeObservedAtUtc = timeObservedAtUtc == null ? observation.get("observed_on").getTextValue() : timeObservedAtUtc;
                if (timeObservedAtUtc == null) {
                    getLogger().warn(study, "failed to retrieve observation time for [" + targetTaxonName + "]");
                } else {
                    DateTime dateTime = parseUTCDateTime(timeObservedAtUtc);
                    nodeFactory.setUnixEpochProperty(collectedRel, dateTime.toDate());
                }

            }
        }

    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
