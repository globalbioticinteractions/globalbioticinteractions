package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForINaturalistTest extends GraphDBTestCase {


    @Test
    public void importTestResponse() throws IOException, NodeFactoryException, StudyImporterException {
        StudyImporterForINaturalist importer = new StudyImporterForINaturalist();

        Study study = nodeFactory.getOrCreateStudy("iNaturalist", "Ken-ichi Kueda", "http://inaturalist.org", "", "iNaturalist is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ", "");

        InputStream resourceAsStream = getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json");
        ObjectMapper mapper = new ObjectMapper();
        assertThat(resourceAsStream, is(not(nullValue())));
        JsonNode array = mapper.readTree(resourceAsStream);
        if (!array.isArray()) {
            throw new StudyImporterException("expected json array, but found object");
        }
        for (int i = 0; i < array.size(); i++) {
            parseSingleInteractions(study, array.get(i));
        }

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(array.size(), is(count));

        Taxon sourceTaxonNode = nodeFactory.findTaxon("Crepidula fornicata");


        assertThat(sourceTaxonNode, is(not(nullValue())));
        Iterable<Relationship> relationships = sourceTaxonNode.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        for (Relationship relationship : relationships) {
            Node predatorSpecimen = relationship.getStartNode();
            Relationship ateRel = predatorSpecimen.getSingleRelationship(InteractType.ATE, Direction.OUTGOING);
            Node preySpecimen = ateRel.getEndNode();
            assertThat(preySpecimen, is(not(nullValue())));
            Relationship preyClassification = preySpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
            assertThat((String) preyClassification.getEndNode().getProperty("name"), is(any(String.class)));

            Relationship locationRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            if (locationRel != null) {
                assertThat((Double) locationRel.getEndNode().getProperty("latitude"), is(any(Double.class)));
                assertThat((Double) locationRel.getEndNode().getProperty("longitude"), is(any(Double.class)));
            }

            Relationship collectedRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
            assertThat((Long) collectedRel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(any(Long.class)));

            assertThat((String) collectedRel.getStartNode().getProperty(Study.CONTRIBUTOR), is("Ken-ichi Kueda"));
        }
    }

    private void parseSingleInteractions(Study study, JsonNode jsonNode) throws NodeFactoryException, StudyImporterException {
        JsonNode sourceTaxon = jsonNode.get("taxon");
        String sourceTaxonName = sourceTaxon.get("name").getTextValue();

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

        String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
        timeObservedAtUtc = timeObservedAtUtc == null ? observation.get("observed_on").getTextValue() : timeObservedAtUtc;
        if (timeObservedAtUtc == null) {
            throw new StudyImporterException("failed to retrieve observation time for [" + sourceTaxonName + "]");
        }
        DateTime dateTime = parseUTCDateTime(timeObservedAtUtc);
        nodeFactory.setUnixEpochProperty(study.collected(sourceSpecimen), dateTime.toDate());
        JsonNode targetTaxon = observation.get("taxon");
        String targetTaxonName = targetTaxon.get("name").getTextValue();
        if (!"taxon".equals(interactionDataType)) {
            throw new StudyImporterException("expected [taxon] as observation_type datatype, but found [" + interactionDataType + "]");
        }

        if ("Eating".equals(interactionType)) {
            sourceSpecimen.ate(nodeFactory.createSpecimen(targetTaxonName));
        } else if ("Host".equals(interactionType)) {
            sourceSpecimen.interactsWith(nodeFactory.createSpecimen(targetTaxonName), InteractType.HOST_OF);
        } else {
            throw new StudyImporterException("found unsupported interactionType [" + interactionType + "]");
        }
    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
