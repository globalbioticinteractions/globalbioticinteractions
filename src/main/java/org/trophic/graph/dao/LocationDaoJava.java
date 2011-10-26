package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Graph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;

import java.util.ArrayList;
import java.util.List;

public class LocationDaoJava extends SuperDao implements LocationDao {

    @Override
    public List<Location> getLocations() {
        List<Location> result = new ArrayList<Location>();
        GraphDatabaseService graph = GraphService.getGraphService();
        Transaction tx = graph.beginTx();
        try{
            Index<Node> index = graph.index().forNodes("studies");
            IndexHits<Node> studies = index.get("title", "mississippiAlabamaFishDiet.csv.gz");
            for (Node study: studies){
                Traverser studyTraverser = getTraverserWithRelType(study, RelTypes.COLLECTED);
                while (studyTraverser.iterator().hasNext()){
                    Node collectedSpecimen = studyTraverser.iterator().next();
                    Traverser locationTraverser = getTraverserWithRelType(collectedSpecimen, RelTypes.COLLECTED_AT);
                    while (locationTraverser.iterator().hasNext()){
                        Node collectedAtLocation = locationTraverser.iterator().next();
                        Location location = createLocation(collectedAtLocation);
                        result.add(location);
                    }
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
