package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForWood extends StudyImporterNodesAndLinks {

    public StudyImporterForWood(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            importLinks(getDataset().getResource(getLinksResourceName()), new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger()), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }
    }

    public void importLinks(InputStream inputStream, InteractionListener interactionListener, ImportFilter filter) throws IOException, StudyImporterException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(inputStream));

        while ((filter == null || filter.shouldImportRecord((long) parser.lastLineNumber())) && parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }


    private Map<String, String> importLink(LabeledCSVParser parser) {
        Map<String, String> link = new TreeMap<String, String>();
        addTSN(parser, link, "PredTSN", StudyImporterForTSV.SOURCE_TAXON_ID);
        link.put(StudyImporterForTSV.SOURCE_TAXON_NAME, parser.getValueByLabel("PredName"));
        addTSN(parser, link, "PreyTSN", StudyImporterForTSV.TARGET_TAXON_ID);
        link.put(StudyImporterForTSV.TARGET_TAXON_NAME, parser.getValueByLabel("PreyName"));
        link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getSourceCitationLastAccessed());
        link.put(StudyImporterForTSV.REFERENCE_CITATION, getSourceCitation());
        link.put(StudyImporterForTSV.REFERENCE_ID, getSourceDOI());
        link.put(StudyImporterForTSV.REFERENCE_DOI, getSourceDOI());
        link.put(StudyImporterForTSV.REFERENCE_URL, getSourceDOI());
        if (getLocality() != null) {
            link.put(StudyImporterForTSV.LOCALITY_NAME, getLocality().getName());
            link.put(StudyImporterForTSV.LOCALITY_ID, getLocality().getId());
        }
        if (getLocation() != null) {
            link.put(StudyImporterForTSV.DECIMAL_LATITUDE, Double.toString(getLocation().getLat()));
            link.put(StudyImporterForTSV.DECIMAL_LONGITUDE, Double.toString(getLocation().getLng()));
        }
        link.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, "preysOn");
        link.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "RO:0002439");
        return link;
    }

    private static void addTSN(LabeledCSVParser parser, Map<String, String> link, String tsn, String tsnLabel) {
        String tsnValue = parser.getValueByLabel(tsn);
        if (!StringUtils.startsWith(tsnValue, "san")) {
            link.put(tsnLabel, "ITIS:" + tsnValue);
        }
    }

}
