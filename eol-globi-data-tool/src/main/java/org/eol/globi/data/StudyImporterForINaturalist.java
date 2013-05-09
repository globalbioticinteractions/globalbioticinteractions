package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.InputStream;

public class StudyImporterForINaturalist extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForINaturalist.class);

    public StudyImporterForINaturalist(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        InputStream resourceAsStream = getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json");

        Study study = nodeFactory.getOrCreateStudy("iNaturalist", "Ken-ichi Kueda", "http://inaturalist.org", "", "iNaturalist is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ", "");

        parseJSON(resourceAsStream, study);
        return study;
    }

    protected int parseJSON(InputStream resourceAsStream, Study study) throws StudyImporterException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = null;
        try {
            array = mapper.readTree(resourceAsStream);
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
    public void setImportFilter(ImportFilter importFilter) {

    }

    private void parseSingleInteractions(Study study, JsonNode jsonNode) throws NodeFactoryException, StudyImporterException {
        JsonNode sourceTaxon = jsonNode.get("taxon");
        JsonNode sourceTaxonNode = sourceTaxon.get("name");
        if (sourceTaxonNode == null) {
            LOG.warn("skipping interaction with missing source taxon name [" + sourceTaxon + "]");
        } else {
            String sourceTaxonName = sourceTaxonNode.getTextValue();

            JsonNode observationField = jsonNode.get("observation_field");
            String interactionDataType = observationField.get("datatype").getTextValue();
            String interactionType = observationField.get("name").getTextValue();

            JsonNode observation = jsonNode.get("observation");
            Specimen sourceSpecimen = nodeFactory.createSpecimen(sourceTaxonName);
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
                LOG.warn("failed to retrieve observation time for [" + sourceTaxonName + "]");
            } else {
                DateTime dateTime = parseUTCDateTime(timeObservedAtUtc);
                nodeFactory.setUnixEpochProperty(collectedRel, dateTime.toDate());
            }
            JsonNode targetTaxon = observation.get("taxon");
            if (targetTaxon == null) {
                LOG.warn("cannot create interaction with missing target taxon name for observation with id [" + observation.get("id") + "]");
            } else {
                JsonNode targetTaxonNameNode = targetTaxon.get("name");
                String targetTaxonName = targetTaxonNameNode.getTextValue();
                if (!"taxon".equals(interactionDataType)) {
                    throw new StudyImporterException("expected [taxon] as observation_type datatype, but found [" + interactionDataType + "]");
                }


                if ("Eating".equals(interactionType)) {
                    sourceSpecimen.ate(nodeFactory.createSpecimen(targetTaxonName));
                } else if ("Host".equals(interactionType)) {
                    sourceSpecimen.interactsWith(nodeFactory.createSpecimen(targetTaxonName), InteractType.HOST_OF);
                } else if ("Flower species".equals(interactionType)) {
                    sourceSpecimen.interactsWith(nodeFactory.createSpecimen(targetTaxonName), InteractType.POLLINATES);
                } else if ("Perching on".equals(interactionType)) {
                    sourceSpecimen.interactsWith(nodeFactory.createSpecimen(targetTaxonName), InteractType.PERCHING_ON);
                } else if ("Pollinating".equals(interactionType)) {
                    sourceSpecimen.interactsWith(nodeFactory.createSpecimen(targetTaxonName), InteractType.POLLINATES);
                } else if ("Other Species in Group".equals(interactionType)) {
                    LOG.warn("interactionType [" + interactionDataType + "] not supported");
                } else {
                    throw new StudyImporterException("found unsupported interactionType [" + interactionType + "]");
                }
            }
        }

    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
