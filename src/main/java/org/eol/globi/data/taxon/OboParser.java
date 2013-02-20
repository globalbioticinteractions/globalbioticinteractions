package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;

public class OboParser implements TaxonParser {

    public static final String OBO_NAME = "name: ";
    public static final String OBO_IS_A = "is_a: ";
    public static final String OBO_ID = "id: ";
    public static final String HAS_RANK = "property_value: has_rank NCBITaxon:";
    public static final String URN_LSID_PREFIX = "NCBITaxon:";

    @Override
    public void parse(BufferedReader bufferedReader, TaxonImportListener listener) throws IOException {
        TaxonTerm currentTerm = null;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if ("[Term]".equals(line)) {
                if (currentTerm != null && currentTerm.getRank() != null) {
                    listener.addTerm(currentTerm.getName(), Long.parseLong(currentTerm.getId()));
                }
                currentTerm = new TaxonTerm();
            }

            if (currentTerm != null) {
                if (line.startsWith(OBO_ID)) {
                    currentTerm.setId(line.substring(OBO_ID.length() + "NCBITaxon:".length()));
                } else if (line.startsWith(OBO_IS_A)) {
                    currentTerm.setIsA(line.substring(OBO_IS_A.length()));
                } else if (line.startsWith(OBO_NAME)) {
                    currentTerm.setName(line.substring(OBO_NAME.length()));
                } else if (line.startsWith(HAS_RANK)) {
                    currentTerm.setRank(line.substring(HAS_RANK.length()));
                }
            }
        }
    }

    @Override
    public int getExpectedMaxTerms() {
        return 798595;
    }
}
