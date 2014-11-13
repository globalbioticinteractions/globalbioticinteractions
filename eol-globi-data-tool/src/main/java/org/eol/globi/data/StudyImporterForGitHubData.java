package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.service.GitHubDataFinder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class StudyImporterForGitHubData extends BaseStudyImporter {
    public StudyImporterForGitHubData(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {

        try {
            List<String> repositories = GitHubDataFinder.find();
            for (String repository : repositories) {
                importData(repository);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to discover github data repositories", e);
        } catch (URISyntaxException e) {
            throw new StudyImporterException("failed to discover github data repositories", e);
        }
        return null;
    }

    protected void importData(String repo) throws StudyImporterException {
        try {
            String descriptor = "https://raw.githubusercontent.com/" + repo + "/master/globi.json";
            JsonNode desc = new ObjectMapper().readTree(new URL(descriptor).openStream());
            String sourceCitation = desc.get("citation").getValueAsText();
            String dataUrl = "https://raw.githubusercontent.com/" + repo + "/master/interactions.tsv";
            LabeledCSVParser parser = parserFactory.createParser(dataUrl, "UTF-8");
            parser.changeDelimiter('\t');
            while (parser.getLine() != null) {
                String referenceCitation = parser.getValueByLabel("referenceCitation");
                String referenceDoi = parser.getValueByLabel("referenceDoi");
                Study study = nodeFactory.getOrCreateStudy(referenceCitation, null, null, null, referenceCitation, null, sourceCitation + ReferenceUtil.createLastAccessedString(dataUrl), referenceDoi);
                study.setCitationWithTx(referenceCitation);

                String sourceTaxonId = StringUtils.trimToNull(parser.getValueByLabel("sourceTaxonId"));
                String sourceTaxonName = StringUtils.trim(parser.getValueByLabel("sourceTaxonName"));

                String targetTaxonId = StringUtils.trimToNull(parser.getValueByLabel("targetTaxonId"));
                String targetTaxonName = StringUtils.trim(parser.getValueByLabel("targetTaxonName"));

                String interactionTypeId = StringUtils.trim(parser.getValueByLabel("interactionTypeId"));
                if (StringUtils.isNotBlank(targetTaxonName)
                        && StringUtils.isNotBlank(sourceTaxonName)) {
                    if (StringUtils.equals("RO:0002444", interactionTypeId)) {
                        Specimen source = nodeFactory.createSpecimen(sourceTaxonName, sourceTaxonId);
                        Specimen target = nodeFactory.createSpecimen(targetTaxonName, targetTaxonId);
                        source.interactsWith(target, InteractType.PARASITE_OF);
                        study.collected(source);
                    } else {
                        throw new StudyImporterException("unsupported interaction type id [" + interactionTypeId + "]");
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import repo [" + repo + "]", e);
        }

    }

}
