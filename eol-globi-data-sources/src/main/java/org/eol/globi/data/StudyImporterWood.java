package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterWood extends BaseStudyImporter {

    public StudyImporterWood(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        return null;
    }

    public static void importLinks(InputStream inputStream, InteractionListener interactionListener) throws IOException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(inputStream));

        while (parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }


    private static Map<String, String> importLink(LabeledCSVParser parser) {
        Map<String, String> link = new TreeMap<String, String>();
        addTSN(parser, link, "PredTSN", "source_taxon_external_id");
        link.put("source_taxon_name", parser.getValueByLabel("PredName"));
        addTSN(parser, link, "PreyTSN", "target_taxon_external_id");
        link.put("target_taxon_name", parser.getValueByLabel("PreyName"));
        link.put("study_source_citation", "Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.g1qr6");
        link.put("study_citation", "Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Effects of spatial scale of sampling on food web structure. Ecology and Evolution, online in advance of print. http://dx.doi.org/10.1002/ece3.1640");
        link.put("study_doi", "doi:10.1002/ece3.1640");
        link.put("study_url", "http://dx.doi.org/10.1002/ece3.1640");
        link.put("locality_name", "Sanak Island, Alaska, USA");
        link.put("locality_id", "GEONAMES:5873327");
        link.put("latitude", "54.42972");
        link.put("longitude", "-162.70889");
        link.put("interaction_type_name", "preysOn");
        link.put("interaction_type_id", "RO:0002439");
        return link;
    }

    private static void addTSN(LabeledCSVParser parser, Map<String, String> link, String tsn, String tsnLabel) {
        String tsnValue = parser.getValueByLabel(tsn);
        if (!StringUtils.startsWith(tsnValue, "san")) {
            link.put(tsnLabel, "ITIS:" + tsnValue);
        }
    }
}
