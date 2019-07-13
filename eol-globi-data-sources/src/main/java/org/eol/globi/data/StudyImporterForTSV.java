package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForTSV extends StudyImporterWithListener {
    public static final String INTERACTION_TYPE_ID = "interactionTypeId";
    public static final String TARGET_TAXON_ID = "targetTaxonId";
    public static final String TARGET_TAXON_NAME = "targetTaxonName";
    public static final String SOURCE_TAXON_NAME = "sourceTaxonName";
    public static final String SOURCE_TAXON_ID = "sourceTaxonId";
    public static final String STUDY_SOURCE_CITATION = "studySourceCitation";
    public static final String REFERENCE_ID = "studyTitle";
    public static final String REFERENCE_DOI = "referenceDoi";
    public static final String REFERENCE_CITATION = "referenceCitation";
    public static final String BASIS_OF_RECORD_ID = "basisOfRecordId";
    public static final String BASIS_OF_RECORD_NAME = "basisOfRecordName";
    public static final String DECIMAL_LATITUDE = "decimalLatitude";
    public static final String DECIMAL_LONGITUDE = "decimalLongitude";
    public static final String LOCALITY_ID = "localityId";
    public static final String REFERENCE_URL = "referenceUrl";
    public static final String LOCALITY_NAME = "localityName";
    public static final String INTERACTION_TYPE_NAME = "interactionTypeName";
    public static final String HABITAT_NAME = "habitatName";
    public static final String HABITAT_ID = "habitatId";
    public static final String SOURCE_OCCURRENCE_ID = "sourceOccurrenceId";
    public static final String TARGET_OCCURRENCE_ID = "targetOccurrenceId";
    public static final String SOURCE_BODY_PART_ID = "sourceBodyPartId";
    public static final String SOURCE_BODY_PART_NAME = "sourceBodyPartName";
    public static final String TARGET_BODY_PART_ID = "targetBodyPartId";
    public static final String TARGET_BODY_PART_NAME = "targetBodyPartName";
    public static final String SOURCE_LIFE_STAGE_ID = "sourceLifeStageId";
    public static final String SOURCE_LIFE_STAGE_NAME = "sourceLifeStageName";
    public static final String TARGET_LIFE_STAGE_ID = "targetLifeStageId";
    public static final String TARGET_LIFE_STAGE_NAME = "targetLifeStageName";
    public static final String ASSOCIATED_TAXA = "associatedTaxa";
    public static final String ARGUMENT_TYPE_ID = "argumentTypeId";

    private static final String RESOURCE_LINE_NUMBER = "resourceLineNumber";
    private static final String RESOURCE_URI = "resourceURI";

    public String getBaseUrl() {
        return getDataset().getArchiveURI().toString();
    }

    public StudyImporterForTSV(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            importRepository(getRepositoryName(), getSourceCitation());
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
    }


    private void importRepository(String namespace, String sourceCitation) throws IOException, StudyImporterException {
        ArrayList<IOException> parserExceptions = new ArrayList<>();
        importResource(namespace, sourceCitation, "/interactions.tsv", '\t', parserExceptions);
        importResource(namespace, sourceCitation, "/interactions.csv", ',', parserExceptions);
        if (parserExceptions.size() > 1) {
            throw parserExceptions.get(0);
        }
    }

    private void importResource(String namespace, String sourceCitation, String resourceName, char newDelim, List<IOException> parserExceptions) throws IOException, StudyImporterException {
        String resourceURIString = getDataset().getResourceURI(resourceName).toString();
        LabeledCSVParser parser = null;
        try {
            parser = parserFactory.createParser(resourceURIString, "UTF-8");
            parser.changeDelimiter(newDelim);
        } catch (IOException ex) {
            parserExceptions.add(new IOException("failed to access [" + resourceURIString + "]", ex));
        }
        if (parser != null) {
            importResource(namespace, sourceCitation, getInteractionListener(), resourceURIString, parser);
        }
    }

    private void importResource(String namespace, String sourceCitation, InteractionListener interactionListenerImpl, String resourceURIString, LabeledCSVParser parser) throws IOException, StudyImporterException {
        while (parser.getLine() != null) {
            final Map<String, String> link = new TreeMap<>();
            final String referenceDoi = StringUtils.replace(parser.getValueByLabel(REFERENCE_DOI), " ", "");
            putNotBlank(link, REFERENCE_DOI, referenceDoi);
            putNotBlank(link, REFERENCE_CITATION, CSVTSVUtil.valueOrNull(parser, REFERENCE_CITATION));
            putNotBlank(link, REFERENCE_URL, CSVTSVUtil.valueOrNull(parser, REFERENCE_URL));
            putNotBlank(link, STUDY_SOURCE_CITATION, CitationUtil.sourceCitationLastAccessed(getDataset(), sourceCitation == null ? "" : sourceCitation + ". "));

            putNotBlank(link, SOURCE_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(SOURCE_TAXON_ID)));
            putNotBlank(link, SOURCE_TAXON_NAME, StringUtils.trim(parser.getValueByLabel(SOURCE_TAXON_NAME)));
            putNotBlank(link, TARGET_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(TARGET_TAXON_ID)));
            putNotBlank(link, TARGET_TAXON_NAME, StringUtils.trim(parser.getValueByLabel(TARGET_TAXON_NAME)));
            putNotBlank(link, INTERACTION_TYPE_ID, StringUtils.trim(parser.getValueByLabel(INTERACTION_TYPE_ID)));
            putNotBlank(link, DECIMAL_LATITUDE, StringUtils.trim(parser.getValueByLabel(DECIMAL_LATITUDE)));
            putNotBlank(link, DECIMAL_LONGITUDE, StringUtils.trim(parser.getValueByLabel(DECIMAL_LONGITUDE)));
            putNotBlank(link, LOCALITY_ID, StringUtils.trim(parser.getValueByLabel(LOCALITY_ID)));
            putNotBlank(link, LOCALITY_NAME, StringUtils.trim(parser.getValueByLabel(LOCALITY_NAME)));
            putNotBlank(link, SOURCE_BODY_PART_ID, StringUtils.trim(parser.getValueByLabel(SOURCE_BODY_PART_ID)));
            putNotBlank(link, SOURCE_BODY_PART_NAME, StringUtils.trim(parser.getValueByLabel(SOURCE_BODY_PART_NAME)));
            putNotBlank(link, TARGET_BODY_PART_ID, StringUtils.trim(parser.getValueByLabel(SOURCE_BODY_PART_ID)));
            putNotBlank(link, TARGET_BODY_PART_NAME, StringUtils.trim(parser.getValueByLabel(SOURCE_BODY_PART_NAME)));

            String argumentTypeId = StringUtils.trim(parser.getValueByLabel(ARGUMENT_TYPE_ID));
            if (StringUtils.isBlank(argumentTypeId)) {
                String negated = StringUtils.trim(parser.getValueByLabel("isNegated"));
                argumentTypeId = StringUtils.equalsIgnoreCase(negated, "true")
                        ? PropertyAndValueDictionary.REFUTES
                        : PropertyAndValueDictionary.SUPPORTS;
            }
            putNotBlank(link, ARGUMENT_TYPE_ID, argumentTypeId);

            putNotBlank(link, RESOURCE_LINE_NUMBER, Integer.toString(parser.getLastLineNumber()));
            putNotBlank(link, RESOURCE_URI, resourceURIString);

            attemptToGenerateReferencePropertiesIfMissing(namespace, link);

            interactionListenerImpl.newLink(link);
        }
    }

    private void attemptToGenerateReferencePropertiesIfMissing(String namespace, Map<String, String> link) {
        if (!ExternalIdUtil.hasProperty(link, REFERENCE_CITATION)) {
            putNotBlank(link, REFERENCE_CITATION, generateReferenceCitation(link));
        }
        if (!ExternalIdUtil.hasProperty(link, REFERENCE_ID)) {
            putNotBlank(link, REFERENCE_ID, namespace + generateReferenceId(link));
        }
    }

    private void putNotBlank(Map<String, String> link, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            link.put(key, value);
        }
    }

    public String getRepositoryName() {
        return getDataset().getNamespace();
    }

    protected static String generateReferenceId(Map<String, String> props) {
        String[] candidateIdsInIncreasingPreference = {STUDY_SOURCE_CITATION,
                REFERENCE_CITATION,
                REFERENCE_URL,
                REFERENCE_DOI,
                REFERENCE_ID};
        return ExternalIdUtil.selectValue(props, candidateIdsInIncreasingPreference);
    }

    protected static String generateReferenceCitation(Map<String, String> props) {
        String[] candidateIdsInIncreasingPreference = {
                STUDY_SOURCE_CITATION,
                REFERENCE_ID,
                REFERENCE_URL,
                REFERENCE_DOI,
                REFERENCE_CITATION
        };
        return ExternalIdUtil.selectValue(props, candidateIdsInIncreasingPreference);
    }

}
