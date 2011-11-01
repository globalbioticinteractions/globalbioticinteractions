package org.trophic.graph.dao;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.dto.SpecimenDto;

import java.util.ArrayList;
import java.util.List;

public class SpecimentDaoJava extends SuperDao implements SpecimenDao {

    private static final String[] studieNames = {"mississippiAlabamaFishDiet.csv.gz", "lavacaBayTrophicData.csv.gz"};

    @Override
    public List<SpecimenDto> getSpecimens(String[] studies) {
        if (studies == null || studies.length == 0){
            studies = new String[2];
            System.arraycopy(studieNames, 0, studies, 0, studieNames.length );
        }
        List<SpecimenDto> result = new ArrayList<SpecimenDto>();
        GraphDatabaseService graph = GraphService.getGraphService();
        Transaction tx = graph.beginTx();
        try{
            Index<Node> index = graph.index().forNodes("studies");
            for (String studyName: studies){
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