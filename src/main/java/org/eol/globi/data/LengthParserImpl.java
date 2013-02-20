package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

public class LengthParserImpl implements LengthParser {

    private String lengthColumnLabel;

    public LengthParserImpl(String lengthColumnLabel) {
        this.lengthColumnLabel = lengthColumnLabel;
    }

    @Override
    public Double parseLengthInMm(LabeledCSVParser csvParser) throws StudyImporterException {
        Double lengthInMm = null;
        String valueByLabel = csvParser.getValueByLabel(lengthColumnLabel);
        try {
            return valueByLabel == null ? null : Double.parseDouble(valueByLabel);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to createTaxon specimen length [" + lengthInMm + "]");
        }
    }
}
