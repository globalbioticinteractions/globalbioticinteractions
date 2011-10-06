package org.trophic.graph.data;

import org.trophic.graph.domain.Study;

import java.util.HashMap;
import java.util.Map;

public class StudyLibrary {
    public static final String MISSISSIPPI_ALABAMA = "mississippiAlabamaFishDiet.csv.gz";
    public static final String LAVACA_BAY = "lavacaBayTrophicData.csv.gz";

    public static final Map<String, Map<String, String>> COLUMN_MAPPERS = new HashMap<String, Map<String, String>>() {{
        put(MISSISSIPPI_ALABAMA, new HashMap<String, String>() {{
            put(StudyImporterImpl.LATITUDE, "lat");
            put(StudyImporterImpl.LONGITUDE, "long");
            put(StudyImporterImpl.DEPTH, "depth");
            put(StudyImporterImpl.SEASON, "season");
            put(StudyImporterImpl.PREY_SPECIES, "prey");
            put(StudyImporterImpl.PREDATOR_SPECIES, "predator");
        }});
        put(LAVACA_BAY, new HashMap<String, String>() {{
            put(StudyImporterImpl.SEASON, "Season");
            put(StudyImporterImpl.PREY_SPECIES, "Predator Species");
            put(StudyImporterImpl.PREDATOR_SPECIES, "Prey Item Species");
        }});
    }};
}
