package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class ReferenceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ReferenceUtil.class);
    public static final String REFERENCE_PREFIX = "reference";

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

    public static String generateReferenceCitation(Map<String, String> properties) {
        StringBuilder citation = new StringBuilder();
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.AUTHOR, citation, ", ");
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.YEAR, citation, ". ");
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.TITLE, citation, ". ");
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.JOURNAL, citation,  properties.containsKey(DatasetImporterForMetaTable.VOLUME) || properties.containsKey(DatasetImporterForMetaTable.NUMBER) || properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ". ");
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.VOLUME, citation,  properties.containsKey(DatasetImporterForMetaTable.NUMBER) ? "(" : (properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ". "));
        appendWithOptionalPrefixForKey(properties, DatasetImporterForMetaTable.NUMBER, citation,  properties.containsKey(DatasetImporterForMetaTable.VOLUME) ? ")" : "");
        if (properties.containsKey(DatasetImporterForMetaTable.NUMBER)) {
            citation.append(properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ".");
        }
        String value = properties.get(DatasetImporterForMetaTable.PAGES);
        if (StringUtils.isBlank(value)) {
            value = properties.get(REFERENCE_PREFIX + DatasetImporterForMetaTable.PAGES);
        }
        append(citation, value, ". ", "pp.");


        String citationFromId = null;
        if (properties.containsKey(DatasetImporterForTSV.REFERENCE_DOI)) {
            String str = properties.get(DatasetImporterForTSV.REFERENCE_DOI);
            if (StringUtils.isNoneBlank(str)) {
                try {
                    DOI doi = DOI.create(str);
                    citationFromId = doi.toPrintableDOI();
                } catch (MalformedDOIException e) {
                    // ignore malformed DOIs here
                }
            }
        }

        if (StringUtils.isBlank(citationFromId) && properties.containsKey(DatasetImporterForTSV.REFERENCE_URL)) {
            citationFromId = properties.get(DatasetImporterForTSV.REFERENCE_URL);
        }

        if (StringUtils.isNoneBlank(citationFromId)) {
            citation.append(citationFromId);
        }

        return StringUtils.trim(citation.toString());
    }

    private static void appendWithOptionalPrefixForKey(Map<String, String> properties, String key, StringBuilder citation, String continuation) {
        String value = getString(properties, key);
        append(citation, value, continuation);
    }

    private static String getString(Map<String, String> properties, String key) {
        String value = properties.get(key);
        if (StringUtils.isBlank(value)) {
            value = properties.get(REFERENCE_PREFIX + key);
        }
        return value;
    }

    public static void append(StringBuilder citation, String value, String suffix, String prefix) {
        if (StringUtils.isNotBlank(value)) {
            citation.append(prefix);
            citation.append(value);
            citation.append(suffix);
        }
    }

    public static void append(StringBuilder citation, String value, String continuation) {
        append(citation, value, continuation, "");
    }
}
