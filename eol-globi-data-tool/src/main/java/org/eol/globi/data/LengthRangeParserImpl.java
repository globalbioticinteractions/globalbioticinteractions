package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

public class LengthRangeParserImpl implements LengthParser {
    private final String lengthColumnLabel;

    public LengthRangeParserImpl(String lengthColumnLabel) {
        this.lengthColumnLabel = lengthColumnLabel;
    }

    @Override
    public Double parseLengthInMm(LabeledCSVParser csvParser) throws StudyImporterException {
        String lengthStringString =   csvParser.getValueByLabel(lengthColumnLabel);
        Double averageLengthInMm = null;
        if (null != lengthStringString && lengthStringString.trim().length() > 0) {
            String[] split = lengthStringString.split("-");
            if (split.length == 2) {
                averageLengthInMm = parseAndAverage(csvParser, split);
            } else {
                throw new StudyImporterException("malformed range format [" + lengthStringString + "]");
            }
        }
        return averageLengthInMm;
    }

    private Double parseAndAverage(LabeledCSVParser csvParser, String[] split) throws StudyImporterException {
        Double averageLengthInMm;
        try {
            Double minLengthInMm = Double.parseDouble(split[0]);
            Double maxLengthInMm = Double.parseDouble(split[1]);
            averageLengthInMm = (maxLengthInMm + minLengthInMm) / 2.0d;
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to createTaxon specimen length range [" + csvParser + "]");
        }
        return averageLengthInMm;
    }
}
