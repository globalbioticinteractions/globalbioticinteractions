package org.eol.globi.opentree;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

public class OpenTreeUtil {

    public static void readTaxonomy(OpenTreeListener listener, InputStream inputStream) throws IOException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(new CSVParser(IOUtils.toBufferedInputStream(inputStream), '\t'));
        while (parser.getLine() != null) {
            String taxonId = parser.getValueByLabel("uid");
            String[] externalIds = StringUtils.split(parser.getValueByLabel("sourceinfo"), ",");
            for (String otherTaxonId : externalIds) {
                listener.taxonSameAs(taxonId, otherTaxonId);
            }
        }
    }

    public static void extractIdsFromTree(TaxonListener listener, InputStream inputStream) {
        Pattern idPattern = Pattern.compile("_ott[0-9]+");
        Scanner scanner = new Scanner(inputStream, "UTF-8");
        scanner.useDelimiter("[\\),]");
        String taxonId;
        while ((taxonId = scanner.findWithinHorizon(idPattern, 0)) != null) {
            listener.addTaxonId(taxonId);
        }
    }
}
