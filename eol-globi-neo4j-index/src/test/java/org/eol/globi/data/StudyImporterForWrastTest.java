package org.eol.globi.data;

import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForWrastTest extends GraphDBTestCase {

    @Test
    public void createAndPopulateStudyFromLavacaBay() throws StudyImporterException, ParseException {
        String csvString =
                "\"Month\",\"Day\",\"Year\",\"Region\",\"Season\",\"Habitat\",\"Site\",\"Family\",\"Predator Species\",\"TL (mm)\",\"Prey Item Species\",\"Prey item\",\"Number\",\"Condition Index\",\"Volume\",\"Percent Content\",\"Prey Item Trophic Level\",\"Notes\",\"Call #\"\n";
        csvString += "7,24,2001,\"Lower\",\"Fall\",\"Marsh\",1,\"Sciaenidae\",\"Sciaenops ocellatus\",420,\"Acrididae spp. \",\"Acrididae \",1,\"III\",0.4,3.2520325203,2.5,,1\n";
        csvString += "7,25,2001,\"Lower\",\"Spring\",\"Non-Veg \",1,\"Ariidae\",\"Arius felis\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,,2\n";
        csvString += "7,26,2001,\"Upper\",\"Spring\",\"Reef\",2,\"Depth\",\"Missing depth\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,,2\n";

        Map<String, String> contentMap = new HashMap<String, String>();
        String locationString = "\"Location\",\"Latitude\",\"Longitude\",,\"Region\",\"Habitat\",\"Site\"\n" +
                "\"LM1\",28.595267,-96.477033,,\"Lower\",\"Marsh edge\",1\n" +
                "\"LSG1\",28.596233,-96.476483,,\"Lower\",\"Marsh edge\",1\n" +
                "\"LM2\",28.593150,-96.474633,,\"Lower\",\"Marsh edge\",2\n" +
                "\"LSG2\",28.594833,-96.473967,,\"Lower\",\"Marsh edge\",2\n" +
                "\"LNV1A\",28.608417,-96.475517,,\"Lower\",\"Non-Veg \",1\n" +
                "\"LNV1B\",28.607217,-96.474500,,\"Lower\",\"Non-Veg \",1\n" +
                "\"LNV2A\",28.592400,-96.483033,,\"Lower\",\"Non-Veg \",2\n" +
                "\"LNV2B\",28.590883,-96.484133,,\"Lower\",\"Non-Veg \",2\n" +
                "\"UR2B\",28.656483,-96.597217,,\"Upper\",\"Reef\",2";

        String envString = "\"Date\",\"Season\",\"Upper/Lower\",\"Habitat\",\"Site\",\"Air Temp (ºC)\",\"Wind Chill (ºC)\",\"Relative Humidity (%)\",\"Heat Index (ºC)\",\"Dew Point (ºC)\",\"Max Wind intensity (mph)\",\"Ave Wind intensity (mph)\",\"Wind Direction\",\"Cloud Cover (%)\",\"Rain?\",\"Depth (m)\",\"Temp (ºC) Surface\",\"Temp (ºC) Bottom\",\"Mean Temp Surface/Bottom\",\"pH Surface\",\"pH Bottom\",\"Mean pH Surface/Bottom\",\"DO (mg/L) Surface\",\"DO (mg/L) Bottom\",\"Mean DO Surface/Bottom\",\"Sal (o/oo) Surface\",\"Sal (o/oo) Bottom\",\"Mean Sal Surface/Bottom\",\"Secchi (m)\"\n" +
                "7/24/2006,\"Summer\",\"L\",\"R\",1,\"81.3 (F)\",\"81.5 (F)\",71.8,\"86.2 (F)\",\"70.6 (F)\",8.5,8.1,\"SE\",75,\"Yes\",1.9,30.79,30.34,30.565,8.11,8.13,8.12,5.16,4.8,4.98,22.8,23.07,22.935,0.4\n" +
                "7/24/2006,\"Summer\",\"L\",\"R\",2,\"81.3 (F)\",\"81.5 (F)\",71.8,\"86.2 (F)\",\"70.6 (F)\",8.5,8.1,\"SE\",75,\"Yes\",2.1,31.07,30.14,30.605,8.11,8.1,8.105,5.14,4.64,4.89,23.37,23.84,23.605,0.48\n" +
                "10/26/2006,\"Fall\",\"L\",\"M\",1,26.8,26.5,78,30,22.4,17.9,15,\"S \",95,\"No\",0.8,25.57,,25.57,8.29,,8.29,8.32,,8.32,21.26,,21.26,\"Bottom\"\n" +
                "10/26/2006,\"Fall\",\"L\",\"M\",2,26.8,26.5,78,30,22.4,17.9,15,\"S \",95,\"No\",0.4,25.84,,25.84,8.32,,8.32,8.21,,8.21,21.47,,21.47,\"Bottom\"\n" +
                "10/26/2006,\"Fall\",\"L\",\"SG\",1,26.8,26.5,78,30,22.4,17.9,15,\"S \",95,\"No\",0.8,25.57,,25.57,8.29,,8.29,8.32,,8.32,21.26,,21.26,\"Bottom\"\n" +
                "4/27/2007,\"Spring\",\"L\",\"R\",1,22.4,22.3,75,23.1,18.1,10.4,9.1,\"NW\",98,\"No\",1.6,23.43,23.39,23.41,,,,7.31,7.09,7.2,14.64,14.64,14.64,0.32\n" +
                "4/27/2007,\"Spring\",\"L\",\"R\",2,22.4,22.3,75,23.1,18.1,10.4,9.1,\"NW\",98,\"No\",2,23.77,23.77,23.77,,,,7.01,6.82,6.915,15.04,15.04,15.04,0.38\n" +
                "4/27/2007,\"Spring\",\"L\",\"NV\",1,22.4,22.3,75,23.1,18.1,10.4,9.1,\"NW\",98,\"No\",2,23.85,23.85,23.85,,,,6.96,6.81,6.885,14.65,14.65,14.65,0.48\n" +
                "4/27/2007,\"Spring\",\"L\",\"NV\",2,22.4,22.3,75,23.1,18.1,10.4,9.1,\"NW\",98,\"No\",1.8,23.78,23.78,23.78,,,,6.87,6.65,6.76,15.11,15.18,15.145,0.45";
        contentMap.put(StudyImporterForWrast.LAVACA_BAY_LOCATIONS, locationString);
        contentMap.put(StudyImporterForWrast.LAVACA_BAY_DATA_SOURCE, csvString);
        contentMap.put(StudyImporterForWrast.LAVACA_BAY_ENVIRONMENTAL, envString);

        StudyImporterForWrast importer = new StudyImporterForWrast(new TestParserFactory(contentMap), nodeFactory);


        importStudy(importer);
        Study study = getStudySingleton(getGraphDb());


        int specimenCount = 0;
        for (Relationship specimen : NodeUtil.getSpecimens(study)) {
            specimenCount++;
        }

        assertThat(specimenCount, is(5));


        assertNotNull(taxonIndex.findTaxonByName("Sciaenops ocellatus"));
        assertNotNull(taxonIndex.findTaxonByName("Arius felis"));

        assertNotNull(taxonIndex.findTaxonByName("Aegathoa oculata"));

        assertNotNull(nodeFactory.findSeason("spring"));
        assertNotNull(nodeFactory.findSeason("fall"));

        Study foundStudy = nodeFactory.findStudy("Wrast 2008");
        assertNotNull(foundStudy);
        for (Relationship rel : NodeUtil.getSpecimens(study)) {
            Date unixEpochProperty = nodeFactory.getUnixEpochProperty(new SpecimenNode(rel.getEndNode()));
            SimpleDateFormat simpleDateFormat = StudyImporterForWrast.getSimpleDateFormat();
            Date endDate = simpleDateFormat.parse("7/27/2001");
            Date startDate = simpleDateFormat.parse("7/23/2001");
            assertThat(unixEpochProperty.before(endDate), is(true));
            assertThat(unixEpochProperty.after(startDate), is(true));
            Specimen specimen = new SpecimenNode(rel.getEndNode());

            for (Relationship ateRel : NodeUtil.getStomachContents(specimen)) {
                TaxonNode taxon = new TaxonNode(rel.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode());
                String scientificName = taxon.getName();
                if ("Sciaenops ocellatus".equals(scientificName)) {
                    Location location = specimen.getSampleLocation();
                    assertThat(location, is(not(nullValue())));
                    assertThat(location.getLatitude(), is((28.595267 + 28.596233) / 2.0));
                    assertThat(location.getLongitude(), is((-96.477033 - 96.476483) / 2.0));
                    assertThat(location.getAltitude(), is(-0.8));

                    Iterable<Relationship> stomachContents = NodeUtil.getStomachContents(specimen);
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Node endNode = containsRel.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode();
                        Object name = endNode.getProperty("name");
                        String name1 = (String) name;
                        assertThat(name1.replaceAll("\\W", ""), is("Acrididae"));
                        count++;
                    }
                    assertEquals(1, count);
                    Season season = specimen.getSeason();
                    assertEquals("fall", season.getTitle());
                    assertEquals(420.0d, specimen.getLengthInMm());


                } else if ("Arius felis".equals(scientificName)) {
                    Location location = specimen.getSampleLocation();
                    assertThat(location, is(not(nullValue())));
                    assertThat(location.getLatitude(), is((28.608417 + 28.607217) / 2.0));
                    assertThat(location.getLongitude(), is((-96.475517 - 96.474500) / 2.0));
                    assertThat(location.getAltitude(), is(-2.0d));

                    Iterable<Relationship> stomachContents = NodeUtil.getStomachContents(specimen);
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Object name = containsRel.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode().getProperty("name");
                        assertEquals("Aegathoa oculata", name);
                        count++;
                    }
                    assertEquals(2, count);

                    Season season = specimen.getSeason();
                    assertEquals("spring", season.getTitle());

                    assertEquals(176.0d, specimen.getLengthInMm());
                } else if ("Missing depth".equals(scientificName)) {
                    Location location = specimen.getSampleLocation();
                    assertThat(location, is(not(nullValue())));
                    assertThat(location.getAltitude(), is(nullValue()));
                } else {
                    fail("unexpected scientificName of predator [" + scientificName + "]");
                }

            }

        }
    }

    @Test
    public void testImportFullFile() throws StudyImporterException {
        StudyImporterForWrast importer = new StudyImporterForWrast(new ParserFactoryLocal(), nodeFactory);
        importStudy(importer);
    }

    @Test
    public void depthId() {
        String depthId = StudyImporterForWrast.createDepthId("Summer", "Upper", "1", "Marsh");
        assertThat(depthId, is("MarshUpper1Summer"));
        depthId = StudyImporterForWrast.createDepthId("Summer", "U", "1", "M");
        assertThat(depthId, is("MarshUpper1Summer"));
        depthId = StudyImporterForWrast.createDepthId("Summer", "U", "1", "Non-Veg ");
        assertThat(depthId, is("Non-VegUpper1Summer"));
        depthId = StudyImporterForWrast.createDepthId("Summer", "U", "1", "Non-Veg ");
        assertThat(depthId, is("Non-VegUpper1Summer"));
        depthId = StudyImporterForWrast.createDepthId("Summer", "U", "1", "NV ");
        assertThat(depthId, is("Non-VegUpper1Summer"));
        depthId = StudyImporterForWrast.createDepthId("Summer", "U", "1", "R");
        assertThat(depthId, is("ReefUpper1Summer"));
    }
}
