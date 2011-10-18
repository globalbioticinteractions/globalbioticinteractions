package org.trophic.graph.data;

import java.util.Map;

public class LengthParserFactory {

    public LengthParser createParser(String studyTitle) throws StudyImporterException {
        LengthParser parser = null;
        Map<String, String> columnMapper = StudyLibrary.COLUMN_MAPPERS.get(studyTitle);
        if (StudyLibrary.LAVACA_BAY.equals(studyTitle)) {
            parser = new LengthParserImpl(columnMapper.get(StudyImporterImpl.LENGTH_IN_MM));
        } else if (StudyLibrary.MISSISSIPPI_ALABAMA.equals(studyTitle)) {
            parser = new LengthRangeParserImpl(columnMapper.get(StudyImporterImpl.LENGTH_RANGE_IN_MM));
        } else {
            throw new StudyImporterException("no length parser factory for study [" + studyTitle + "]");
        }
        return parser;
    }
}
