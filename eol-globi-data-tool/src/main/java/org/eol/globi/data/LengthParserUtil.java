package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

import java.util.Map;

public class LengthParserUtil {
    public static Double parseLengthInMm(LengthParser parser, LabeledCSVParser csvParser, Map<String, String> columnToNormalizedTermMapper) throws StudyImporterException {

        parser.parseLengthInMm(csvParser);
        String lengthRangeInMm = csvParser.getValueByLabel(columnToNormalizedTermMapper.get(StudyImporterForSimons.LENGTH_RANGE_IN_MM));
        Double averageLengthInMm = null;
        if (null != lengthRangeInMm) {
            String[] split = lengthRangeInMm.split("-");
            if (split.length == 2) {
                try {
                    Double minLengthInMm = Double.parseDouble(split[0]);
                    Double maxLengthInMm = Double.parseDouble(split[1]);
                    averageLengthInMm = (maxLengthInMm + minLengthInMm) / 2.0d;
                } catch (NumberFormatException ex1) {
                    throw new StudyImporterException("failed to createTaxon specimen length range [" + lengthRangeInMm + "]");
                }
            }
        }
        Double lengthInMm = averageLengthInMm;
        if (null == lengthInMm) {
            Double lengthInMm1 = null;
            try {
                lengthInMm1 = Double.parseDouble(csvParser.getValueByLabel(columnToNormalizedTermMapper.get(StudyImporterForSimons.LENGTH_IN_MM)));
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("failed to createTaxon specimen length [" + lengthInMm1 + "]");
            }
            lengthInMm = lengthInMm1;
        }
        return lengthInMm;
    }

}
