package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

public interface LengthParser {
    Double parseLengthInMm(LabeledCSVParser parser) throws StudyImporterException;
}
