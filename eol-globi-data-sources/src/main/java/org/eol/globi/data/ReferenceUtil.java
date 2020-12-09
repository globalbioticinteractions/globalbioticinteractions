package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ReferenceUtil.class);

    public static Map<String, String> buildRefMap(ParserFactory parserFactory, URI referencePath) throws StudyImporterException {
        return buildRefMap(parserFactory, referencePath, "short", "full", ',');
    }

    protected static Map<String, String> buildRefMap(ParserFactory parserFactory, URI referencePath, String keyColumnName, String valueColumnName, char delimiter) throws StudyImporterException {
        LabeledCSVParser referenceParser;
        try {
            referenceParser = parserFactory.createParser(referencePath, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + referencePath + "]", e);
        }
        referenceParser.changeDelimiter(delimiter);
        return buildRefMap(referenceParser, referencePath, keyColumnName, valueColumnName);
    }

    public static Map<String, String> buildRefMap(LabeledCSVParser referenceParser, URI referencePath, String keyColumnName, String valueColumnName) throws StudyImporterException {
        Map<String, String> refMap = new TreeMap<>();
        try {
            while (referenceParser.getLine() != null) {
                String shortReference = referenceParser.getValueByLabel(keyColumnName);
                if (StringUtils.isBlank(shortReference)) {
                    LOG.warn("missing short reference on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                } else {
                    String fullReference = referenceParser.getValueByLabel(valueColumnName);
                    if (StringUtils.isBlank(fullReference)) {
                        LOG.warn("missing full reference for [" + shortReference + "] on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]");
                        fullReference = shortReference;
                    }
                    if (StringUtils.isBlank(refMap.get(StringUtils.trim(shortReference)))) {
                        refMap.put(StringUtils.trim(shortReference), StringUtils.trim(fullReference));
                    } else {
                        LOG.warn("skipping [" + shortReference + "] on line [" + referenceParser.lastLineNumber() + "] in [" + referencePath + "]: key already defined.");
                    }

                }

            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + referencePath + "]", e);
        }
        return refMap;
    }

}
