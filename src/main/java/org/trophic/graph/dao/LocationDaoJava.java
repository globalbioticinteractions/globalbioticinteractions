package org.trophic.graph.dao;

import com.tinkerpop.blueprints.pgm.Graph;
import org.neo4j.graphdb.*;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.RelTypes;

import java.util.List;

public class LocationDaoJava implements LocationDao {

    @Override
    public List<Location> getLocations() {
        GraphDatabaseService graph = GraphService.getGraphService();
        Transaction tx = graph.beginTx();
        try{
            Node referenceNode = graph.getReferenceNode();
            Traverser traverser = referenceNode.traverse(
                    Traverser.Order.BREADTH_FIRST,
                    StopEvaluator.END_OF_GRAPH,
                    ReturnableEvaluator.ALL_BUT_START_NODE,
                    RelTypes.COLLECTED,
                    Direction.OUTGOING);
//            Index<Node> index = graph.index().forNodes("");
//            index.get();
            while (traverser.iterator().hasNext()){
                Node node = (Node)traverser.iterator().next();
                String type = (String) node.getProperty("type");
                System.out.println("Type: " + type);
            }
        } finally {
            tx.finish();
        }
        return null;
    }

    @Override
    public void setGraph(Graph graph) {

    }

    @Override
    public String sayHallo() {
        return "Hallo";
    }

}
