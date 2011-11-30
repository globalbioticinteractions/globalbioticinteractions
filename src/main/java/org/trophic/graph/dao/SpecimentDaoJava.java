package org.trophic.graph.dao;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.trophic.graph.data.StudyLibrary;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.dto.SpecimenDto;

import java.util.ArrayList;
import java.util.List;

public class SpecimentDaoJava extends SuperDao implements SpecimenDao {

    private GraphDatabaseService graphService;

    public SpecimentDaoJava(GraphDatabaseService graphService) {
        this.graphService = graphService;
    }

    @Override
    public List<SpecimenDto> getSpecimens(String[] studies) {
        List<SpecimenDto> result = new ArrayList<SpecimenDto>();
        Transaction tx = graphService.beginTx();
        try{
            Index<Node> index = graphService.index().forNodes("studies");
            for (String studyName: StudyLibrary.COLUMN_MAPPERS.keySet()){
                IndexHits<Node> studyIndexHit = index.get("title", studyName);
                for (Node study: studyIndexHit){
                    Traverser studyTraverser = getTraverserWithRelType(study, RelTypes.COLLECTED);
                    while (studyTraverser.iterator().hasNext()){
                        Node collectedSpecimen = studyTraverser.iterator().next();
                        SpecimenDto specimenDto = createSpecimen(collectedSpecimen);
                        if (specimenDto != null)
                            result.add(specimenDto);
                    }
                }
            }
        } finally {
            tx.finish();
        }
        return result;
    }

}