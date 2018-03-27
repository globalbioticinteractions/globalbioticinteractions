package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.Study;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.NodeUtil;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForICESTest extends GraphDBTestCase {
    @Test
    public void importOneEveryThousandLines() throws StudyImporterException {
        StudyImporterForICES studyImporterFor = new StudyImporterForICES(new ParserFactoryLocal(), nodeFactory);
        studyImporterFor.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 1000 == 0;
            }
        });
        studyImporterFor.importStudy();

        List<Study> studies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(studies.size(), is(1));
        Study study = studies.get(0);

        Iterator<Relationship> specimens = NodeUtil.getSpecimens(study).iterator();
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

        StudyImporterForICES importer = new StudyImporterForICES(new TestParserFactory(firstBunchOfLines), nodeFactory);

        importStudy(importer);
        Study study = getStudySingleton(getGraphDb());

        assertNotNull(taxonIndex.findTaxonByName("Gadus morhua"));
        assertNotNull(taxonIndex.findTaxonByName("Polychaeta"));
        assertNotNull(taxonIndex.findTaxonByName("Nereis"));

        Iterable<Relationship> collectedRels = NodeUtil.getSpecimens(study);
        int specimenCollected = 0;
        int preyEaten = 0;
        for (Relationship rel : collectedRels) {
            assertThat(rel.getProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH), is(DateUtil.parsePatternUTC("1981", "yyyy").toDate().getTime()));
            Node specimen = rel.getEndNode();
            assertNotNull(specimen);
            Iterable<Relationship> relationships = specimen.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE));
            for (Relationship ignored : relationships) {
                assertThat(specimen.getProperty(SpecimenConstant.LENGTH_IN_MM), is(125.0));
                preyEaten++;
            }

            Relationship collectedAtRelationship = specimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING);
            assertNotNull(collectedAtRelationship);
            Node locationNode = collectedAtRelationship.getEndNode();
            assertNotNull(locationNode);
            assertThat((Double) locationNode.getProperty(LocationConstant.LATITUDE), is(55.25));
            assertThat((Double) locationNode.getProperty(LocationConstant.LONGITUDE), is(8.5));
            specimenCollected++;

        }

        assertThat(specimenCollected, Is.is(3));
        assertThat(preyEaten, Is.is(2));

    }

}
