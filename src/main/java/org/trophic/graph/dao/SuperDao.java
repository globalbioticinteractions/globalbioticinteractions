package org.trophic.graph.dao;

import org.neo4j.graphdb.*;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.dto.SpecimenDto;

public abstract class SuperDao {

    protected Traverser getTraverserWithRelType(Node node, RelationshipType relationshipType){
        Traverser traverser = node.traverse(
                        Traverser.Order.BREADTH_FIRST,
                        StopEvaluator.END_OF_GRAPH,
                        ReturnableEvaluator.ALL_BUT_START_NODE,
                        relationshipType,
                        Direction.OUTGOING);
        return traverser;
    }

    protected Location createLocation(Node node){
        Double longitude = (Double) node.getProperty(Location.LONGITUDE);
        Double latitude = (Double) node.getProperty(Location.LATITUDE);
        Double altitude = (Double) node.getProperty(Location.ALTITUDE);
        Location location = new Location(node, longitude, latitude, altitude);
        return location;
    }

    protected SpecimenDto createSpecimen(Node collectedSpecimen){
 		Double lengthInMm = (Double) collectedSpecimen.getProperty("lengthInMm");
        Relationship classifiedAs = collectedSpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        Node species = classifiedAs.getEndNode();
        String speciesName = (String) species.getProperty("name");

        Relationship collectedAt = collectedSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
        if (collectedAt == null){
            System.out.println("No Locations for: " + speciesName);
            return null;
        }
        Node locationPosition = collectedAt.getEndNode();

        Double longitude = (Double) locationPosition.getProperty(Location.LONGITUDE);
        Double latitude = (Double) locationPosition.getProperty(Location.LATITUDE);
        Double altitude = (Double) locationPosition.getProperty(Location.ALTITUDE);

        SpecimenDto speciesDto = new SpecimenDto();
        speciesDto.setAltitude(altitude);
        speciesDto.setLongitude(28.60841D);
        speciesDto.setLatitude(-96.475517D);
        speciesDto.setSpecies( speciesName );
		speciesDto.setLengthInMm(lengthInMm);
        speciesDto.setId( collectedSpecimen.getId() );
        return speciesDto;
    }

}