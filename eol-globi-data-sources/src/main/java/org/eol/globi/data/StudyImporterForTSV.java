package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class StudyImporterForTSV extends StudyImporterWithListener {
    public static final String INTERACTION_TYPE_ID = "interactionTypeId";
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
    public static final String SOURCE_SEX_ID = "sourceSexId";
    public static final String SOURCE_SEX_NAME = "sourceSexName";
    public static final String TARGET_SEX_ID = "targetSexId";
    public static final String TARGET_SEX_NAME = "targetSexName";
    public static final String ASSOCIATED_TAXA = "associatedTaxa";
    public static final String ARGUMENT_TYPE_ID = "argumentTypeId";

    private static final String RESOURCE_LINE_NUMBER = "resourceLineNumber";
    private static final String RESOURCE_URI = "resourceURI";

    public static final String SOURCE_COLLECTION_CODE = "sourceCollectionCode";
    public static final String TARGET_COLLECTION_CODE = "targetCollectionCode";
    public static final String SOURCE_COLLECTION_ID = "sourceCollectionId";
    public static final String TARGET_COLLECTION_ID = "targetCollectionId";
    public static final String SOURCE_INSTITUTION_CODE = "sourceInstitutionCode";
    public static final String TARGET_INSTITUTION_CODE = "targetInstitutionCode";
    public static final String SOURCE_CATALOG_NUMBER = "sourceCatalogNumber";
    public static final String TARGET_CATALOG_NUMBER = "targetCatalogNumber";

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
            throw new IOException("failed to access/parse [/interactions.tsv] and [/interactions.csv]", parserExceptions.get(0));
        }
    }

    private void importResource(String namespace, String sourceCitation, String resourceName, char newDelim, List<IOException> parserExceptions) throws IOException, StudyImporterException {
        URI resourceURI = getDataset().getResourceURI(URI.create(resourceName));
        if (resourceURI == null) {
            parserExceptions.add(new IOException("failed to access [" + resourceName + "] as individual resource (e.g. local/remote data/file)."));
        } else {
            LabeledCSVParser parser = null;
            try {
                parser = parserFactory.createParser(resourceURI, "UTF-8");
                parser.changeDelimiter(newDelim);
            } catch (IOException ex) {
                parserExceptions.add(new IOException("failed to access [" + resourceURI.toString() + "]", ex));
            }
            if (parser != null) {
                importResource(namespace, sourceCitation, getInteractionListener(), resourceURI, parser);
            }
        }
    }

    private void importResource(String namespace, String sourceCitation, InteractionListener interactionListener, URI resourceURI, LabeledCSVParser parser) throws IOException, StudyImporterException {
        while (parser.getLine() != null) {
            final Map<String, String> link = new TreeMap<>();
            final String referenceDoi = StringUtils.replace(parser.getValueByLabel(REFERENCE_DOI), " ", "");
            putNotBlank(link, REFERENCE_DOI, referenceDoi);
            putNotBlank(link, REFERENCE_CITATION, CSVTSVUtil.valueOrNull(parser, REFERENCE_CITATION));
            putNotBlank(link, REFERENCE_URL, CSVTSVUtil.valueOrNull(parser, REFERENCE_URL));
            putNotBlank(link, STUDY_SOURCE_CITATION, CitationUtil.sourceCitationLastAccessed(getDataset(), sourceCitation == null ? "" : sourceCitation + ". "));

            putNotBlank(link, TaxonUtil.SOURCE_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(TaxonUtil.SOURCE_TAXON_ID)));
            putNotBlank(link, TaxonUtil.TARGET_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(TaxonUtil.TARGET_TAXON_ID)));

            Stream.of(
                    TaxonUtil.SOURCE_TAXON_NAME,
                    TaxonUtil.SOURCE_TAXON_NAME,
                    TaxonUtil.TARGET_TAXON_NAME,
                    INTERACTION_TYPE_NAME,
                    INTERACTION_TYPE_ID,
                    DECIMAL_LATITUDE,
                    DECIMAL_LONGITUDE,
                    LOCALITY_ID,
                    LOCALITY_NAME,
                    SOURCE_BODY_PART_ID,
                    SOURCE_BODY_PART_NAME,
                    TARGET_BODY_PART_ID,
                    TARGET_BODY_PART_NAME,
                    SOURCE_LIFE_STAGE_ID,
                    SOURCE_LIFE_STAGE_NAME,
                    TARGET_LIFE_STAGE_ID,
                    TARGET_LIFE_STAGE_NAME,
                    SOURCE_SEX_ID,
                    SOURCE_SEX_NAME,
                    TARGET_SEX_ID,
                    TARGET_SEX_NAME
            )
                    .forEach(x -> doIdentifyMap(parser, link, x));

            String argumentTypeId = StringUtils.trim(parser.getValueByLabel(ARGUMENT_TYPE_ID));
            if (StringUtils.isBlank(argumentTypeId)) {
                String negated = StringUtils.trim(parser.getValueByLabel("isNegated"));
                argumentTypeId = StringUtils.equalsIgnoreCase(negated, "true")
                        ? PropertyAndValueDictionary.REFUTES
                        : PropertyAndValueDictionary.SUPPORTS;
            }
            putNotBlank(link, ARGUMENT_TYPE_ID, argumentTypeId);

            putNotBlank(link, RESOURCE_LINE_NUMBER, Integer.toString(parser.getLastLineNumber()));
            putNotBlank(link, RESOURCE_URI, resourceURI.toString());

            attemptToGenerateReferencePropertiesIfMissing(namespace, link);

            TaxonUtil.enrichTaxonNames(link);
            interactionListener.newLink(link);
        }
    }

    private void doIdentifyMap(LabeledCSVParser parser, Map<String, String> link, String propertyName) {
        putNotBlank(link, propertyName, StringUtils.trim(parser.getValueByLabel(propertyName)));
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
