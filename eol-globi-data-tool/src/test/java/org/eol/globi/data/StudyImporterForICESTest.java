package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForICESTest extends GraphDBTestCase {
    @Test
    public void importOneEveryThousandLines() throws StudyImporterException {
        StudyImporterForICES studyImporterFor = new StudyImporterForICES(new ParserFactoryImpl(), nodeFactory);
        studyImporterFor.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 1000 == 0;
            }
        });
        Study study = studyImporterFor.importStudy();

        Iterator<Relationship> specimens = study.getSpecimens().iterator();
        int specimenCount = 0;
        while (specimens.hasNext()) {
            specimens.next();
            specimenCount++;
        }
        assertThat(specimenCount, is(388));
    }

    @Test
    public void parseSelection() throws NodeFactoryException, StudyImporterException, ParseException {
        String firstBunchOfLines = "File Name,Latitude,Longitude,Estimated Lat/Lon,Date/Time,Year,Quarter,Month,Day,Time,Station/Haul,Sample Number,ICES StomachID,Depth,Temperature,Country,Ship,ICES Rectangle,Sampling Method,Predator,Predator NODC Code,Predator (mean) Lengh,Predator (mean) Weight,Predator (mean) Age,Predator Lower Length Bound,Predator Upper Length Bound,CPUE,Number Stomachs With Food,Number Stomachs Regurgitated,Number Stomachs With Skeletal Remains,Number Stomachs Empty,Number Stomachs,Digestion Stage,Prey Species Name,Prey NODC Code,Prey Weight,Prey Lower Length Bound,Prey Upper Length Bound,Prey Number,ICES Internal ID\n" +
                "Exccoddatsto_815,55.25,8.5,Yes,01/01/1981 00:00:00,1981,1,1,1,,,26,26,,,,,39F8,Demersal sampling,Gadus morhua,8791030402,125,,,100,150,29,26,0,0,2,28,,Polychaeta,5001000000,1.5,,,5,1\n" +
                "Exccoddatsto_815,55.25,8.5,Yes,01/01/1981 00:00:00,1981,1,1,1,,,26,26,,,,,39F8,Demersal sampling,Gadus morhua,8791030402,125,,,100,150,29,26,0,0,2,28,,Nereis,5001240400,5.3,,,1,2\n" +
                "Exccoddatsto_815,55.25,8.5,Yes,01/01/1981 00:00:00,1981,1,1,1,,,26,26,,,,,39F8,Demersal sampling,Gadus morhua,8791030402,125,,,100,150,29,26,0,0,2,28,,,5515290302,4.8,,,2,3";

        StudyImporterForICES studyImporterFor = new StudyImporterForICES(new TestParserFactory(firstBunchOfLines), nodeFactory);

        Study study = studyImporterFor.importStudy();
        assertNotNull(nodeFactory.findTaxonByName("Gadus morhua"));
        assertNotNull(nodeFactory.findTaxonByName("Polychaeta"));
        assertNotNull(nodeFactory.findTaxonByName("Nereis"));

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int specimenCollected = 0;
        int preyEaten = 0;
        for (Relationship rel : collectedRels) {
            assertThat((Long) rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(new SimpleDateFormat("yyyy").parse("1981").getTime()));
            Node specimen = rel.getEndNode();
            assertNotNull(specimen);
            Iterable<Relationship> relationships = specimen.getRelationships(Direction.OUTGOING, InteractType.ATE);
            for (Relationship relationship : relationships) {
                assertThat((Double) specimen.getProperty(Specimen.LENGTH_IN_MM), is(125.0));
                preyEaten++;
            }

            Relationship collectedAtRelationship = specimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            assertNotNull(collectedAtRelationship);
            Node locationNode = collectedAtRelationship.getEndNode();
            assertNotNull(locationNode);
            assertThat((Double) locationNode.getProperty(Location.LATITUDE), is(55.25));
            assertThat((Double) locationNode.getProperty(Location.LONGITUDE), is(8.5));
            specimenCollected++;

        }

        assertThat(specimenCollected, Is.is(3));
        assertThat(preyEaten, Is.is(2));

    }

}
