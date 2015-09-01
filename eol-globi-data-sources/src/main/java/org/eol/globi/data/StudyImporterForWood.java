package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForWood extends BaseStudyImporter {

    public static final String SOURCE_URL = "http://datadryad.org/bitstream/handle/10255/dryad.93018/WoodEtal_Append1_v2.csv";
    public static final String STUDY_SOURCE_CITATION = "Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.g1qr6 "
            + ReferenceUtil.createLastAccessedString(SOURCE_URL);

    public StudyImporterForWood(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String linkResource = SOURCE_URL;
        try {
            importLinks(ResourceUtil.asInputStream(linkResource, null), new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger()), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException("failed to find: [" + linkResource + "]");
        }
        return null;
    }

    public static void importLinks(InputStream inputStream, InteractionListener interactionListener, ImportFilter filter) throws IOException, StudyImporterException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(inputStream));

        while ((filter == null || filter.shouldImportRecord((long)parser.lastLineNumber())) && parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }


    private static Map<String, String> importLink(LabeledCSVParser parser) {
        Map<String, String> link = new TreeMap<String, String>();
        addTSN(parser, link, "PredTSN", StudyImporterForTSV.SOURCE_TAXON_ID);
        link.put(StudyImporterForTSV.SOURCE_TAXON_NAME, parser.getValueByLabel("PredName"));
        addTSN(parser, link, "PreyTSN", StudyImporterForTSV.TARGET_TAXON_ID);
        link.put(StudyImporterForTSV.TARGET_TAXON_NAME, parser.getValueByLabel("PreyName"));
        link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, STUDY_SOURCE_CITATION);
        link.put(StudyImporterForTSV.REFERENCE_CITATION, "Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Effects of spatial scale of sampling on food web structure. Ecology and Evolution, online in advance of print. http://dx.doi.org/10.1002/ece3.1640");
        link.put(StudyImporterForTSV.REFERENCE_ID, "doi:10.1002/ece3.1640");
        link.put(StudyImporterForTSV.REFERENCE_DOI, "doi:10.1002/ece3.1640");
        link.put(StudyImporterForTSV.REFERENCE_URL, "http://dx.doi.org/10.1002/ece3.1640");
        link.put(StudyImporterForTSV.LOCALITY_NAME, "Sanak Island, Alaska, USA");
        link.put(StudyImporterForTSV.LOCALITY_ID, "GEONAMES:5873327");
        link.put(StudyImporterForTSV.DECIMAL_LATITUDE, "54.42972");
        link.put(StudyImporterForTSV.DECIMAL_LONGITUDE, "-162.70889");
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
