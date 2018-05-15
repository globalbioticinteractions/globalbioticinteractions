package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForTSV extends BaseStudyImporter {
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
    public static final String SOURCE_LIFE_STAGE = "sourceLifeStage";
    public static final String TARGET_LIFE_STAGE = "targetLifeStage";
    public static final String HABITAT_NAME = "habitatName";
    public static final String HABITAT_ID = "habitatId";
    public static final String SOURCE_BODY_PART_ID = "sourceBodyPartId";
    public static final String SOURCE_BODY_PART_NAME = "sourceBodyPartName";
    public static final String TARGET_BODY_PART_ID = "targetBodyPartId";
    public static final String TARGET_BODY_PART_NAME = "targetBodyPartName";
    public static final String ASSOCIATED_TAXA = "associatedTaxa";

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
        InteractionListenerImpl interactionListenerImpl = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
        LabeledCSVParser parser = parserFactory.createParser(getDataset().getResourceURI("/interactions.tsv").toString(), "UTF-8");
        parser.changeDelimiter('\t');
        while (parser.getLine() != null) {
            final Map<String, String> link = new TreeMap<String, String>();
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
