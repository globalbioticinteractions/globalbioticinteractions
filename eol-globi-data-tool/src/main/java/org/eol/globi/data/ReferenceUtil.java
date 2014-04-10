package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceUtil {
    private static final Log LOG = LogFactory.getLog(ReferenceUtil.class);

    public static Map<String, String> buildRefMap(ParserFactory parserFactory1, String referencePath) throws StudyImporterException {
        Map<String, String> refMap = new TreeMap<String, String>();
        try {
            LabeledCSVParser referenceParser = parserFactory1.createParser(referencePath, CharsetConstant.UTF8);
            while (referenceParser.getLine() != null) {
                String shortReference = referenceParser.getValueByLabel("short");
                if (StringUtils.isBlank(shortReference)) {
                    LOG.warn("missing short reference on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                } else {
                    String fullReference = referenceParser.getValueByLabel("full");
                    if (StringUtils.isBlank(fullReference)) {
                        LOG.warn("missing full reference for [" + shortReference + "] on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                        fullReference = shortReference;
                    }
                    refMap.put(shortReference, fullReference);
                }

            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + referencePath + "]", e);
        }
        return refMap;
    }
}
