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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import scala.util.parsing.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForINaturalistTest extends GraphDBTestCase {


    @Test
    public void importTestResponse() throws IOException, NodeFactoryException {
        StudyImporterForINaturalist importer = new StudyImporterForINaturalist();

        Study study = nodeFactory.getOrCreateStudy("iNaturalist", "Ken-ichi Kueda", "http://inaturalist.org", "", "iNaturalist is a place where you can record what you see in nature, meet other nature lovers, and learn about the natural world. ", "");

        InputStream resourceAsStream = getClass().getResourceAsStream("inaturalist/sample_inaturalist_response.json");
        ObjectMapper mapper = new ObjectMapper();
        assertThat(resourceAsStream, is(not(nullValue())));
        JsonNode array = mapper.readTree(resourceAsStream);
        assertThat(array, is(not(nullValue())));
        assertThat(array.isArray(), is(true));
        JsonNode jsonNode = array.get(0);

        JsonNode sourceTaxon = jsonNode.get("taxon");
        assertThat(sourceTaxon, is(not(nullValue())));
        String sourceTaxonName = sourceTaxon.get("name").getTextValue();
        assertThat(sourceTaxonName, is("Crepidula fornicata"));

        JsonNode observationField = jsonNode.get("observation_field");
        assertThat(observationField.get("datatype").getTextValue(), is("taxon"));
        assertThat(observationField.get("name").getTextValue(), is("Eating"));

        JsonNode observation = jsonNode.get("observation");
        Specimen sourceSpecimen = nodeFactory.createSpecimen(sourceTaxonName);
        double latitude = Double.parseDouble(observation.get("latitude").getTextValue());
        double longitude = Double.parseDouble(observation.get("longitude").getTextValue());
        sourceSpecimen.caughtIn(nodeFactory.getOrCreateLocation(latitude, longitude, null));

        String timeObservedAtUtc = observation.get("time_observed_at_utc").getTextValue();
        DateTime dateTime = parseUTCDateTime(timeObservedAtUtc);
        nodeFactory.setUnixEpochProperty(study.collected(sourceSpecimen), dateTime.toDate());
        JsonNode targetTaxon = observation.get("taxon");
        String targetTaxonName = targetTaxon.get("name").getTextValue();
        assertThat(targetTaxonName, is("Calidris alpina"));


        Taxon sourceTaxonNode = nodeFactory.findTaxon("Crepidula fornicata");
        sourceSpecimen.ate(nodeFactory.createSpecimen(targetTaxonName));
        assertThat(sourceTaxonNode, is(not(nullValue())));
        Iterable<Relationship> relationships = sourceTaxonNode.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);
        for (Relationship relationship : relationships) {
            Node predatorSpecimen = relationship.getStartNode();
            Relationship ateRel = predatorSpecimen.getSingleRelationship(InteractType.ATE, Direction.OUTGOING);
            Node preySpecimen = ateRel.getEndNode();
            assertThat(preySpecimen, is(not(nullValue())));
            Relationship preyClassification = preySpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
            assertThat((String) preyClassification.getEndNode().getProperty("name"), is("Calidris alpina"));

            Relationship locationRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            assertThat((Double) locationRel.getEndNode().getProperty("latitude"), is(41.249813));
            assertThat((Double) locationRel.getEndNode().getProperty("longitude"), is(-72.542556));

            Relationship collectedRel = predatorSpecimen.getSingleRelationship(RelTypes.COLLECTED, Direction.INCOMING);
            assertThat((Long)collectedRel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(1325185920000L));

            assertThat((String)collectedRel.getStartNode().getProperty(Study.CONTRIBUTOR), is("Ken-ichi Kueda"));
        }
    }

    private DateTime parseUTCDateTime(String timeObservedAtUtc) {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(timeObservedAtUtc).withZone(DateTimeZone.UTC);
    }
}
