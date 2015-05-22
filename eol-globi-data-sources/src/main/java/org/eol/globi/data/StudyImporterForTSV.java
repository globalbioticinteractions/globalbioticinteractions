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
import java.util.logging.Level;

public class StudyImporterForTSV extends BaseStudyImporter {
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


    private void importRepository(String repo, String sourceCitation, String baseUrl) throws IOException, NodeFactoryException, StudyImporterException {
        String dataUrl = baseUrl + "/interactions.tsv";
        LabeledCSVParser parser = parserFactory.createParser(dataUrl, "UTF-8");
        parser.changeDelimiter('\t');
        while (parser.getLine() != null) {
            String referenceCitation = parser.getValueByLabel("referenceCitation");
            String referenceDoi = StringUtils.replace(parser.getValueByLabel("referenceDoi"), " ", "");
            Study study = nodeFactory.getOrCreateStudy2(repo + referenceCitation, (sourceCitation == null ? "" : sourceCitation + ". ") + ReferenceUtil.createLastAccessedString(dataUrl), referenceDoi);
            study.setCitationWithTx(referenceCitation);

            String sourceTaxonId = StringUtils.trimToNull(parser.getValueByLabel("sourceTaxonId"));
            String sourceTaxonName = StringUtils.trim(parser.getValueByLabel("sourceTaxonName"));

            String targetTaxonId = StringUtils.trimToNull(parser.getValueByLabel("targetTaxonId"));
            String targetTaxonName = StringUtils.trim(parser.getValueByLabel("targetTaxonName"));

            String interactionTypeId = StringUtils.trim(parser.getValueByLabel("interactionTypeId"));
            if (StringUtils.isNotBlank(targetTaxonName)
                    && StringUtils.isNotBlank(sourceTaxonName)) {
                InteractType type = InteractType.typeOf(interactionTypeId);
                if (type == null) {
                    study.appendLogMessage("unsupported interaction type id [" + interactionTypeId + "]", Level.WARNING);
                } else {
                    Specimen source = nodeFactory.createSpecimen(study, sourceTaxonName, sourceTaxonId);
                    Specimen target = nodeFactory.createSpecimen(study, targetTaxonName, targetTaxonId);
                    source.interactsWith(target, type, getOrCreateLocation(parser, study));
                }
            }
        }
    }

    private Location getOrCreateLocation(LabeledCSVParser parser, Study study) throws IOException, NodeFactoryException {
        LatLng centroid = null;
        String latitude = StringUtils.trim(parser.getValueByLabel("decimalLatitude"));
        String longitude = StringUtils.trim(parser.getValueByLabel("decimalLongitude"));
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                centroid = LocationUtil.parseLatLng(latitude, longitude);
            } catch (InvalidLocationException e) {
                getLogger().warn(study, "found invalid location: [" + e.getMessage() + "]");
            }
        }
        if (centroid == null) {
            String localityId = StringUtils.trim(parser.getValueByLabel("localityId"));
            if (StringUtils.isNotBlank(localityId)) {
                centroid = getGeoNamesService().findLatLng(localityId);
            }
        }
        return centroid == null ? null : nodeFactory.getOrCreateLocation(centroid.getLat(), centroid.getLng(), null);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
}
