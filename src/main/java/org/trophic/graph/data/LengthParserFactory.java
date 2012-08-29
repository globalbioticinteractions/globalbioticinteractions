package org.trophic.graph.data;

import java.util.Map;

public class LengthParserFactory {

    public LengthParser createParser(String studyTitle) throws StudyImporterException {
        LengthParser parser = null;
        Map<String, String> columnMapper = StudyImporterForMississippiAlabama.COLUMN_MAPPERS.get(studyTitle);
        if (StudyImporterForMississippiAlabama.LAVACA_BAY_DATA_SOURCE.equals(studyTitle)) {
            parser = new LengthParserImpl(columnMapper.get(StudyImporterForMississippiAlabama.LENGTH_IN_MM));
        } else if (StudyImporterForMississippiAlabama.MISSISSIPPI_ALABAMA_DATA_SOURCE.equals(studyTitle)) {
            parser = new LengthRangeParserImpl(columnMapper.get(StudyImporterForMississippiAlabama.LENGTH_RANGE_IN_MM));
        } else {
            throw new StudyImporterException("no length parser factory for study [" + studyTitle + "]");
        }
        return parser;
    }
}
