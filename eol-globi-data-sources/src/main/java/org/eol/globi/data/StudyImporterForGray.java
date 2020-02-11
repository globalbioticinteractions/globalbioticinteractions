package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForGray extends StudyImporterWithListener {

    public StudyImporterForGray(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        String resourceName = "links";
        try (InputStream links = getDataset().retrieve(URI.create(resourceName))) {
            importLinks(links, getInteractionListener(), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException("failed to find: [" + resourceName + "]", e);
        }
    }

    public void importLinks(InputStream inputStream, InteractionListener interactionListener, ImportFilter filter) throws IOException, StudyImporterException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createExcelCSVParse(inputStream));

        while ((filter == null || filter.shouldImportRecord((long) parser.lastLineNumber())) && parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }


    private Map<String, String> importLink(LabeledCSVParser parser) {
        Map<String, String> link = new TreeMap<>();
        link.put(TaxonUtil.SOURCE_TAXON_NAME, parser.getValueByLabel("consumer"));
        link.put(StudyImporterForTSV.SOURCE_LIFE_STAGE_NAME, nonNAValueOrNull(parser.getValueByLabel("consumer.lifestage")));
        link.put(TaxonUtil.TARGET_TAXON_NAME, parser.getValueByLabel("resource"));
        link.put(StudyImporterForTSV.TARGET_LIFE_STAGE_NAME, nonNAValueOrNull(parser.getValueByLabel("resource.lifestage")));
        link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getSourceCitationLastAccessed());
        link.put(StudyImporterForTSV.REFERENCE_CITATION, parser.getValueByLabel("full.source"));
        link.put(StudyImporterForTSV.REFERENCE_ID, getSourceDOI() + "/source.id/" + parser.getValueByLabel("source.id"));
        link.put(StudyImporterForTSV.BASIS_OF_RECORD_NAME, parser.getValueByLabel("link.evidence"));
        link.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "eats");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "RO:0002470");
        return link;
    }

    private String nonNAValueOrNull(String value) {
        return StringUtils.equals("NA", value) ? null : value;
    }

}
