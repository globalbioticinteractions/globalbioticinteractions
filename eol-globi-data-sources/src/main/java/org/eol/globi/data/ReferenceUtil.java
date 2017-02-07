package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.Dataset;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class ReferenceUtil {
    private static final Log LOG = LogFactory.getLog(ReferenceUtil.class);

    public static Map<String, String> buildRefMap(ParserFactory parserFactory, String referencePath) throws StudyImporterException {
        return buildRefMap(parserFactory, referencePath, "short", "full", ',');
    }

    protected static Map<String, String> buildRefMap(ParserFactory parserFactory, String referencePath, String keyColumnName, String valueColumnName, char delimiter) throws StudyImporterException {
        Map<String, String> refMap = new TreeMap<String, String>();
        try {
            LabeledCSVParser referenceParser = parserFactory.createParser(referencePath, CharsetConstant.UTF8);
            referenceParser.changeDelimiter(delimiter);
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

    public static String createLastAccessedString(String reference) {
        return "Accessed at <" + StringUtils.trim(reference) + "> on " + new DateTime().toString("dd MMM YYYY") + ".";
    }

    public static String sourceCitationLastAccessed(Dataset dataset, String sourceCitation) {
        String resourceURI = dataset.getOrDefault("url", dataset.getArchiveURI().toString());
        return StringUtils.trim(sourceCitation) + separatorFor(sourceCitation) + createLastAccessedString(resourceURI);
    }

    public static String sourceCitationLastAccessed(Dataset dataset) {
        return sourceCitationLastAccessed(dataset, dataset.getCitation());
    }

    public static String separatorFor(String citationPart) {
        String separator = " ";
        if (!StringUtils.endsWith(StringUtils.trim(citationPart), ".")) {
            separator = ". ";
        }
        return separator;
    }

    public static String citationFor(Dataset dataset) {
        String defaultCitation = "<" + dataset.getArchiveURI().toString() + ">";
        String citation = citationOrDefaultFor(dataset, defaultCitation);

        if (!StringUtils.contains(citation, "doi.org") && !StringUtils.contains(citation, "doi:")) {
            String citationTrimmed = StringUtils.trim(defaultString(citation));
            String doiTrimmed = defaultString(dataset.getDOI());
            if (StringUtils.isBlank(doiTrimmed)) {
                citation = citationTrimmed;
            } else {
                citation = citationTrimmed + separatorFor(citationTrimmed) + doiTrimmed;
            }
        }
        return StringUtils.trim(citation);
    }

    public static String citationOrDefaultFor(Dataset dataset, String defaultCitation) {
        return dataset.getOrDefault(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION, dataset.getOrDefault("citation", defaultCitation));
    }
}
