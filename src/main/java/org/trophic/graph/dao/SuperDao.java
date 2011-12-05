package org.trophic.graph.dao;

import org.neo4j.graphdb.*;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Taxon;
import org.trophic.graph.dto.SpecimenDto;

public abstract class SuperDao {

    protected Traverser getTraverserWithRelType(Node node, RelationshipType relationshipType) {
        Traverser traverser = node.traverse(
                Traverser.Order.BREADTH_FIRST,
                StopEvaluator.END_OF_GRAPH,
                ReturnableEvaluator.ALL_BUT_START_NODE,
                relationshipType,
                Direction.OUTGOING);
        return traverser;
    }

    protected Location createLocation(Node node) {
        Double longitude = (Double) node.getProperty(Location.LONGITUDE);
        Double latitude = (Double) node.getProperty(Location.LATITUDE);
        Double altitude = (Double) node.getProperty(Location.ALTITUDE);
        Location location = new Location(node, longitude, latitude, altitude);
        return location;
    }

    protected SpecimenDto createSpecimen(Node collectedSpecimen) {
        Double lengthInMm = null;

        if (collectedSpecimen.hasProperty(Specimen.LENGTH_IN_MM)) {
            lengthInMm = (Double) collectedSpecimen.getProperty(Specimen.LENGTH_IN_MM);
        }
        Relationship classifiedAs = collectedSpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        String taxonName = null;
        if (null != classifiedAs) {
            Node species = classifiedAs.getEndNode();
            if (null != species && species.hasProperty(Taxon.NAME)) {
                taxonName = (String) species.getProperty(Taxon.NAME);
            }

        }

        Relationship collectedAt = collectedSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
        if (collectedAt == null) {
            System.out.println("No Locations for: " + taxonName);
            return null;
        }
        Node locationPosition = collectedAt.getEndNode();

        Double longitude = (Double) locationPosition.getProperty(Location.LONGITUDE);
        Double latitude = (Double) locationPosition.getProperty(Location.LATITUDE);
        Double altitude = (Double) locationPosition.getProperty(Location.ALTITUDE);

        SpecimenDto specimen = new SpecimenDto();
        specimen.setAltitude(altitude);
        specimen.setLongitude(longitude);
        specimen.setLatitude(latitude);
        specimen.setSpecies(taxonName);
        specimen.setLengthInMm(lengthInMm);
        specimen.setId(collectedSpecimen.getId());
        return specimen;
    }

}