package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_CLASS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_FAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_GENUS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_INFRAORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_KINGDOM;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PARVORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PHYLUM;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBCLASS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBFAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBGENUS;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBSPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUBTRIBE;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUPERFAMILY;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SUPERORDER;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_TRIBE;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_CLASS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_FAMILY;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_GENUS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_INFRAORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_KINGDOM;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PARVORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_IDS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PHYLUM;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBCLASS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBFAMILY;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBGENUS;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBSPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUBTRIBE;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUPERFAMILY;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_SUPERORDER;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_TRIBE;

public class DatasetImporterForTSV extends DatasetImporterWithListener {
    public static final String INTERACTION_TYPE_ID = "interactionTypeId";
    public static final String INTERACTION_TYPE_ID_VERBATIM = INTERACTION_TYPE_ID + "Verbatim";

    public static final String DATASET_CITATION = "studySourceCitation";

    @Deprecated
    public static final String STUDY_SOURCE_CITATION = DATASET_CITATION;

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
    public static final String INTERACTION_TYPE_NAME_VERBATIM = INTERACTION_TYPE_NAME + "Verbatim";
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

    public static final String RESOURCE_TYPES = "resourceTypes";
    public static final String SOURCE_RECORD_NUMBER = "sourceRecordNumber";
    public static final String TARGET_RECORD_NUMBER = "targetRecordNumber";

    private static final String RESOURCE_LINE_NUMBER = "resourceLineNumber";
    private static final String RESOURCE_URI = "resourceURI";

    public static final String SOURCE_COLLECTION_CODE = "sourceCollectionCode";
    public static final String TARGET_COLLECTION_CODE = "targetCollectionCode";

    public static final String SOURCE_COLLECTION_ID = "sourceCollectionId";
    public static final String TARGET_COLLECTION_ID = "targetCollectionId";

    public static final String SOURCE_INSTITUTION_CODE = "sourceInstitutionCode";
    public static final String TARGET_INSTITUTION_CODE = "targetInstitutionCode";

    public static final String SOURCE_CATALOG_NUMBER = "sourceCatalogNumber"; // see http://rs.tdwg.org/dwc/terms/catalogNumber
    public static final String TARGET_CATALOG_NUMBER = "targetCatalogNumber"; // see http://rs.tdwg.org/dwc/terms/catalogNumber

    public static final String SOURCE_FIELD_NUMBER = "sourceFieldNumber"; // see http://rs.tdwg.org/dwc/terms/fieldNumber
    public static final String TARGET_FIELD_NUMBER = "targetFieldNumber"; // see http://rs.tdwg.org/dwc/terms/fieldNumber

    public static final List<Pair<String, String>> SOURCE_TARGET_PROPERTY_NAME_PAIRS = Arrays.asList(
            Pair.of(SOURCE_LIFE_STAGE_NAME, TARGET_LIFE_STAGE_NAME),
            Pair.of(SOURCE_LIFE_STAGE_ID, TARGET_LIFE_STAGE_ID),
            Pair.of(SOURCE_BODY_PART_NAME, TARGET_BODY_PART_NAME),
            Pair.of(SOURCE_BODY_PART_ID, TARGET_BODY_PART_ID),
            Pair.of(SOURCE_SEX_NAME, TARGET_SEX_NAME),
            Pair.of(SOURCE_SEX_ID, TARGET_SEX_ID),
            Pair.of(SOURCE_TAXON_NAME, TARGET_TAXON_NAME),

            Pair.of(SOURCE_TAXON_KINGDOM, TARGET_TAXON_KINGDOM),
            Pair.of(SOURCE_TAXON_PHYLUM, TARGET_TAXON_PHYLUM),
            Pair.of(SOURCE_TAXON_CLASS, TARGET_TAXON_CLASS),
            Pair.of(SOURCE_TAXON_SUBCLASS, TARGET_TAXON_SUBCLASS),
            Pair.of(SOURCE_TAXON_SUPERORDER, TARGET_TAXON_SUPERORDER),
            Pair.of(SOURCE_TAXON_ORDER, TARGET_TAXON_ORDER),
            Pair.of(SOURCE_TAXON_SUBORDER, TARGET_TAXON_SUBORDER),
            Pair.of(SOURCE_TAXON_INFRAORDER, TARGET_TAXON_INFRAORDER),
            Pair.of(SOURCE_TAXON_PARVORDER, TARGET_TAXON_PARVORDER),
            Pair.of(SOURCE_TAXON_SUPERFAMILY, TARGET_TAXON_SUPERFAMILY),
            Pair.of(SOURCE_TAXON_FAMILY, TARGET_TAXON_FAMILY),
            Pair.of(SOURCE_TAXON_SUBFAMILY, TARGET_TAXON_SUBFAMILY),
            Pair.of(SOURCE_TAXON_TRIBE, TARGET_TAXON_TRIBE),
            Pair.of(SOURCE_TAXON_SUBTRIBE, TARGET_TAXON_SUBTRIBE),
            Pair.of(SOURCE_TAXON_GENUS, TARGET_TAXON_GENUS),
            Pair.of(SOURCE_TAXON_SUBGENUS, TARGET_TAXON_SUBGENUS),
            Pair.of(SOURCE_TAXON_SPECIFIC_EPITHET, TARGET_TAXON_SPECIFIC_EPITHET),
            Pair.of(SOURCE_TAXON_SUBSPECIFIC_EPITHET, TARGET_TAXON_SUBSPECIFIC_EPITHET),

            Pair.of(SOURCE_TAXON_PATH, TARGET_TAXON_PATH),
            Pair.of(SOURCE_TAXON_PATH_NAMES, TARGET_TAXON_PATH_NAMES),
            Pair.of(SOURCE_TAXON_PATH_IDS, TARGET_TAXON_PATH_IDS),
            Pair.of(SOURCE_TAXON_PATH_IDS, TARGET_TAXON_PATH_IDS),
            Pair.of(SOURCE_TAXON_ID, TARGET_TAXON_ID),
            Pair.of(SOURCE_CATALOG_NUMBER, TARGET_CATALOG_NUMBER),
            Pair.of(SOURCE_FIELD_NUMBER, TARGET_FIELD_NUMBER),
            Pair.of(SOURCE_INSTITUTION_CODE, TARGET_INSTITUTION_CODE),
            Pair.of(SOURCE_COLLECTION_CODE, TARGET_COLLECTION_CODE),
            Pair.of(SOURCE_COLLECTION_ID, TARGET_COLLECTION_ID),
            Pair.of(SOURCE_OCCURRENCE_ID, TARGET_OCCURRENCE_ID)
    );

    public String getBaseUrl() {
        return getDataset().getArchiveURI().toString();
    }

    public DatasetImporterForTSV(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
        URI resourceURI = URI.create(resourceName);
        LabeledCSVParser parser = null;
        try {
            InputStream is = getDataset().retrieve(resourceURI);
            if (is == null) {
                parserExceptions.add(new IOException("failed to access [" + resourceName + "] as individual resource (e.g. local/remote data/file)."));
            } else {
                parser = ParserFactoryForDataset.getLabeledCSVParser(is, CharsetConstant.UTF8);
                parser.changeDelimiter(newDelim);
            }
        } catch (IOException ex) {
            parserExceptions.add(new IOException("failed to access [" + resourceURI.toString() + "]", ex));
        }
        if (parser != null) {
            importResource(namespace, sourceCitation, getInteractionListener(), resourceURI, parser);
        }
    }

    private void importResource(String namespace, String sourceCitation, InteractionListener interactionListener, URI resourceURI, LabeledCSVParser parser) throws IOException, StudyImporterException {
        while (parser.getLine() != null) {
            final Map<String, String> link = new TreeMap<>();
            final String referenceDoi = StringUtils.replace(parser.getValueByLabel(REFERENCE_DOI), " ", "");
            InteractUtil.putNotBlank(link, REFERENCE_DOI, referenceDoi);
            InteractUtil.putNotBlank(link, REFERENCE_CITATION, CSVTSVUtil.valueOrNull(parser, REFERENCE_CITATION));
            InteractUtil.putNotBlank(link, REFERENCE_URL, CSVTSVUtil.valueOrNull(parser, REFERENCE_URL));
            InteractUtil.putNotBlank(link, DATASET_CITATION, CitationUtil.sourceCitationLastAccessed(getDataset(), sourceCitation == null ? "" : sourceCitation + ". "));

            InteractUtil.putNotBlank(link, SOURCE_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(SOURCE_TAXON_ID)));
            InteractUtil.putNotBlank(link, TARGET_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(TARGET_TAXON_ID)));

            InteractUtil.putIfKeyNotExistsAndValueNotBlank(link, DatasetImporterForMetaTable.EVENT_DATE, StringUtils.trimToNull(parser.getValueByLabel("observationDateTime")));
            InteractUtil.putIfKeyNotExistsAndValueNotBlank(link, DatasetImporterForMetaTable.EVENT_DATE, StringUtils.trimToNull(parser.getValueByLabel("eventDate")));
            InteractUtil.putIfKeyNotExistsAndValueNotBlank(link, DatasetImporterForMetaTable.EVENT_DATE, StringUtils.trimToNull(parser.getValueByLabel(DatasetImporterForMetaTable.EVENT_DATE)));

            String argumentTypeId = StringUtils.trim(parser.getValueByLabel(ARGUMENT_TYPE_ID));
            if (StringUtils.isBlank(argumentTypeId)) {
                String negated = StringUtils.trim(parser.getValueByLabel("isNegated"));
                argumentTypeId = StringUtils.equalsIgnoreCase(negated, "true")
                        ? PropertyAndValueDictionary.REFUTES
                        : PropertyAndValueDictionary.SUPPORTS;
            }
            InteractUtil.putNotBlank(link, ARGUMENT_TYPE_ID, argumentTypeId);

            InteractUtil.putNotBlank(link, RESOURCE_LINE_NUMBER, Integer.toString(parser.getLastLineNumber()));
            InteractUtil.putNotBlank(link, RESOURCE_URI, resourceURI.toString());

            attemptToGenerateReferencePropertiesIfMissing(namespace, link);

            for (String label : parser.getLabels()) {
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(link, label, parser.getValueByLabel(label));
            }

            interactionListener.on(link);
        }
    }

    private void attemptToGenerateReferencePropertiesIfMissing(String namespace, Map<String, String> link) {
        if (!ExternalIdUtil.hasProperty(link, REFERENCE_CITATION)) {
            InteractUtil.putNotBlank(link, REFERENCE_CITATION, generateReferenceCitation(link));
        }
        if (!ExternalIdUtil.hasProperty(link, REFERENCE_ID)) {
            InteractUtil.putNotBlank(link, REFERENCE_ID, namespace + generateReferenceId(link));
        }
    }

    public String getRepositoryName() {
        return getDataset().getNamespace();
    }

    protected static String generateReferenceId(Map<String, String> props) {
        String[] candidateIdsInIncreasingPreference = {DATASET_CITATION,
                REFERENCE_CITATION,
                REFERENCE_URL,
                REFERENCE_DOI,
                REFERENCE_ID};
        return ExternalIdUtil.selectValue(props, candidateIdsInIncreasingPreference);
    }

    protected static String generateReferenceCitation(Map<String, String> props) {
        String[] candidateIdsInIncreasingPreference = {
                DATASET_CITATION,
                REFERENCE_ID,
                REFERENCE_URL,
                REFERENCE_DOI,
                REFERENCE_CITATION
        };
        return ExternalIdUtil.selectValue(props, candidateIdsInIncreasingPreference);
    }

}
