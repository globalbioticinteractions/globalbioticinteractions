package org.eol.globi.data;

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

public class StudyImporterForINaturalist extends BaseStudyImporter {
    public static final String INATURALIST_URL = "http://inaturalist.org";

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
            int previousResultCount;
            int pageNumber = 1;
            do {
                String uri = "http://www.inaturalist.org/observation_field_values.json?type=taxon&page=" + pageNumber + "&per_page=100&license=any";
                HttpGet get = new HttpGet(uri);
                get.addHeader("accept", "application/json");
                try {
                    HttpResponse response = defaultHttpClient.execute(get);
                    if (response.getStatusLine().getStatusCode() != 200) {
                        throw new StudyImporterException("failed to execute query to [ " + uri + "]: status code [" + response.getStatusLine().getStatusCode() + "]");
                    }
                    previousResultCount = parseJSON(response.getEntity().getContent(), study);
                    pageNumber++;
                    totalInteractions += previousResultCount;
                    getLogger().info(study, "importing [" + pageNumber + "] total [" + totalInteractions + "]");

                } catch (IOException e) {
                    throw new StudyImporterException("failed to execute query to [ " + uri + "]", e);
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

                Relationship collectedRel = study.collected(sourceSpecimen);

                String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
                timeObservedAtUtc = timeObservedAtUtc == null ? observation.get("observed_on").getTextValue() : timeObservedAtUtc;
                if (timeObservedAtUtc == null) {
                    getLogger().warn(study, "failed to retrieve observation time for [" + targetTaxonName + "]");
                } else {
                    DateTime dateTime = parseUTCDateTime(timeObservedAtUtc);
                    nodeFactory.setUnixEpochProperty(collectedRel, dateTime.toDate());
                }

                InteractType type;
                if ("Eating".equals(interactionType) || "With the prey".equals(interactionType)) {
                    type = InteractType.ATE;
                } else if ("Host".equals(interactionType)) {
                    type = InteractType.HAS_HOST;
                } else if ("Flower species".equals(interactionType)) {
                    type = InteractType.POLLINATES;
                } else if ("Perching on".equals(interactionType)) {
                    type = InteractType.PERCHING_ON;
                } else if ("Pollinating".equals(interactionType)) {
                    type = InteractType.POLLINATES;
                } else if ("Other Species in Group".equals(interactionType)) {
                    type = InteractType.INTERACTS_WITH;
                } else if ("Butterfly & Moth Host Plant".equals(interactionType)) {
                    type = InteractType.INTERACTS_WITH;
                } else if ("Butterfly & Moth Nectar Plant".equals(interactionType)) {
                    type = InteractType.INTERACTS_WITH;
                } else if ("Gall Inducer".equals(interactionType)) {
                    type = InteractType.INTERACTS_WITH;
                } else {
                    throw new StudyImporterException("found unsupported interactionType [" + interactionType + "]");
                }
                if (type != null) {
                    sourceSpecimen.interactsWith(targetSpecimen, type);
                }
            }
        }

    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
