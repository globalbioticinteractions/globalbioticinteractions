package org.trophic.graph.dao;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.trophic.graph.data.StudyLibrary;
import org.trophic.graph.domain.RelTypes;
import org.trophic.graph.dto.SpecimenDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpecimentDaoJava extends SuperDao implements SpecimenDao {

    private GraphDatabaseService graphService;
    private HashMap<Double, SpecimenDto> map = new HashMap<Double, SpecimenDto>();

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

                        if (specimenDto == null)
                            continue;

                        SpecimenDto speci = map.get(specimenDto.getLongLat());
                        if (speci == null) {
                            map.put(specimenDto.getLongLat(), specimenDto);
                            result.add(specimenDto);
                        } else {
                            speci.addSpecimen(specimenDto);
                        }
                    }
                }
            }
        } finally {
            tx.finish();
        }
        return result;
    }

    @Override
    public List<SpecimenDto> getSpecimensByLocation(String latitude, String longitude) {
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

                        if (specimenDto == null)
                            continue;

                        String lat = String.valueOf(specimenDto.getLatitude());
                        String lng = String.valueOf(specimenDto.getLongitude());

                        lat = ensureLength(lat);
                        lng = ensureLength(lng);

                        latitude = ensureLength(latitude);
                        longitude = ensureLength(longitude);

                        if (lat.startsWith("29.1245")){
                            System.out.println("close");
                        }

                        if (lat.equals(latitude) && lng.equals(longitude))
                            result.add(specimenDto);
                    }
                }
            }
        } catch (Exception ex){
          ex.printStackTrace();
        } finally {
            tx.finish();
        }
        return result;
    }

    private String ensureLength(String val){
        int length = 14;
        if (val.length() > length)
            val = val.substring(0, length);
        return val;
    }

}