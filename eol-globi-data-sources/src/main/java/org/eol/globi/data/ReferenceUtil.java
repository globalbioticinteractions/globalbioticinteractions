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
        append(citation, properties.get(DatasetImporterForMetaTable.AUTHOR), ", ");
        append(citation, properties.get(DatasetImporterForMetaTable.YEAR), ". ");
        append(citation, properties.get(DatasetImporterForMetaTable.TITLE), ". ");
        append(citation, properties.get(DatasetImporterForMetaTable.JOURNAL), properties.containsKey(DatasetImporterForMetaTable.VOLUME) || properties.containsKey(DatasetImporterForMetaTable.NUMBER) || properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ". ");
        append(citation, properties.get(DatasetImporterForMetaTable.VOLUME), properties.containsKey(DatasetImporterForMetaTable.NUMBER) ? "(" : (properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ". "));
        append(citation, properties.get(DatasetImporterForMetaTable.NUMBER), properties.containsKey(DatasetImporterForMetaTable.VOLUME) ? ")" : "");
        if (properties.containsKey(DatasetImporterForMetaTable.NUMBER)) {
            citation.append(properties.containsKey(DatasetImporterForMetaTable.PAGES) ? ", " : ".");
        }
        append(citation, properties.get(DatasetImporterForMetaTable.PAGES), ". ", "pp.");


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
