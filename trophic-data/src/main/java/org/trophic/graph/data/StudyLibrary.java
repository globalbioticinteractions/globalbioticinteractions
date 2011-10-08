package org.trophic.graph.data;

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
            put(StudyImporterImpl.LENGTH_RANGE_IN_MM, "sizeclass");
        }});
        put(LAVACA_BAY, new HashMap<String, String>() {{
            put(StudyImporterImpl.SEASON, "Season");
            put(StudyImporterImpl.PREY_SPECIES, "Prey Item Species");
            put(StudyImporterImpl.PREDATOR_SPECIES, "Predator Species");
            put(StudyImporterImpl.LENGTH_IN_MM, "TL");
            put(StudyImporterImpl.PREDATOR_FAMILY, "Family");
        }});
    }};
}
