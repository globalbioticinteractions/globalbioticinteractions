package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Graph;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;

import java.util.ArrayList;
import java.util.List;

public class LocationDaoJava implements LocationDao {

    @Override
    public List<Location> getLocations() {
        List<Location> result = new ArrayList<Location>();
        GraphDatabaseService graph = GraphService.getGraphService();
        Transaction tx = graph.beginTx();
        try{
            Index<Node> index = graph.index().forNodes("studies");
            IndexHits<Node> studies = index.get("title", "mississippiAlabamaFishDiet.csv.gz");
            for (Node study: studies){
                Traverser studyTraverser = study.traverse(
                        Traverser.Order.BREADTH_FIRST,
                        StopEvaluator.END_OF_GRAPH,
                        ReturnableEvaluator.ALL_BUT_START_NODE,
                        RelTypes.COLLECTED,
                        Direction.OUTGOING);
                while (studyTraverser.iterator().hasNext()){
                    Node collectedSpecimen = (Node)studyTraverser.iterator().next();

                    Traverser specimenTraverserOutCollectedAt = collectedSpecimen.traverse(
                        Traverser.Order.BREADTH_FIRST,
                        StopEvaluator.END_OF_GRAPH,
                        ReturnableEvaluator.ALL_BUT_START_NODE,
                        RelTypes.COLLECTED_AT,
                        Direction.OUTGOING);
                    while (specimenTraverserOutCollectedAt.iterator().hasNext()){
                        Node collectedAtLocation = (Node)specimenTraverserOutCollectedAt.iterator().next();
                        Double longitude = (Double) collectedAtLocation.getProperty(Location.LONGITUDE);
                        Double latitude = (Double) collectedAtLocation.getProperty(Location.LATITUDE);
                        Double altitude = (Double) collectedAtLocation.getProperty(Location.ALTITUDE);
                        Location location = new Location(collectedAtLocation, longitude, latitude, altitude);
                        result.add(location);
                    }

//                    Traverser specimenTraverserOutClassifiedAs = collectedSpecimen.traverse(
//                        Traverser.Order.BREADTH_FIRST,
//                        StopEvaluator.END_OF_GRAPH,
//                        ReturnableEvaluator.ALL_BUT_START_NODE,
//                        RelTypes.CLASSIFIED_AS,
//                        Direction.OUTGOING);
//                    while (specimenTraverserOutClassifiedAs.iterator().hasNext()){
//                        Node classifiedAsName = (Node)specimenTraverserOutClassifiedAs.iterator().next();
//
//                    }

                }
            }
        } finally {
            tx.finish();
        }
        return result;
    }

    @Override
    public void setGraph(Graph graph) {

    }

    @Override
    public String sayHallo() {
        return "Hallo";
    }

}
