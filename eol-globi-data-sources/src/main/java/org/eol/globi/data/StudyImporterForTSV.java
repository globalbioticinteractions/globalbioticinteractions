package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.InvalidLocationException;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

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
    public static final String DECIMAL_LATITUDE = "decimalLatitude";
    public static final String DECIMAL_LONGITUDE = "decimalLongitude";
    public static final String LOCALITY_ID = "localityId";
    public static final String REFERENCE_URL = "referenceUrl";
    public static final String LOCALITY_NAME = "localityName";
    public static final String INTERACTION_TYPE_NAME = "interactionTypeName";
    public static final String SOURCE_LIFE_STAGE = "sourceLifeStage";
    public static final String TARGET_LIFE_STAGE = "targetLifeStage";
    private String repositoryName;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private String baseUrl;


    public StudyImporterForTSV(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            importRepository(getRepositoryName(), getSourceCitation(), getBaseUrl());
        } catch (IOException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
        return null;
    }


    private void importRepository(String namespace, String sourceCitation, String baseUrl) throws IOException, NodeFactoryException, StudyImporterException {
        InteractionListenerNeo4j interactionListenerNeo4j = new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger());
        LabeledCSVParser parser = parserFactory.createParser(baseUrl + "/interactions.tsv", "UTF-8");
        parser.changeDelimiter('\t');
        while (parser.getLine() != null) {
            final Map<String, String> link = new TreeMap<String, String>();
            link.put(REFERENCE_CITATION, parser.getValueByLabel(REFERENCE_CITATION));
            link.put(REFERENCE_DOI, StringUtils.replace(parser.getValueByLabel("referenceDoi"), " ", ""));
            link.put(STUDY_SOURCE_CITATION, (sourceCitation == null ? "" : sourceCitation + ". ") + ReferenceUtil.createLastAccessedString(baseUrl + "/interactions.tsv"));
            link.put(REFERENCE_ID, namespace + parser.getValueByLabel(REFERENCE_CITATION));
            link.put(SOURCE_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(SOURCE_TAXON_ID)));
            link.put(SOURCE_TAXON_NAME, StringUtils.trim(parser.getValueByLabel(SOURCE_TAXON_NAME)));
            link.put(TARGET_TAXON_ID, StringUtils.trimToNull(parser.getValueByLabel(TARGET_TAXON_ID)));
            link.put(TARGET_TAXON_NAME, StringUtils.trim(parser.getValueByLabel(TARGET_TAXON_NAME)));
            link.put(INTERACTION_TYPE_ID, StringUtils.trim(parser.getValueByLabel(INTERACTION_TYPE_ID)));
            link.put(DECIMAL_LATITUDE, StringUtils.trim(parser.getValueByLabel(DECIMAL_LATITUDE)));
            link.put(DECIMAL_LONGITUDE, StringUtils.trim(parser.getValueByLabel(DECIMAL_LONGITUDE)));
            link.put(LOCALITY_ID, StringUtils.trim(parser.getValueByLabel(LOCALITY_ID)));
            interactionListenerNeo4j.newLink(link);
        }
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

}
