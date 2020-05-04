package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.StudyImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.StudyImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.StudyImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.StudyImporterForTSV.STUDY_SOURCE_CITATION;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;

public class StudyImporterForSzoboszlai extends StudyImporterWithListener {

    public StudyImporterForSzoboszlai(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    Map<Integer, LatLng> importShapes2() throws IOException {
        InputStream localities = getDataset().retrieve(URI.create("classpath:/org/eol/globi/data/szoboszlai/szoboszlai-localities.tsv"));

        CSVParse tsvParser = CSVTSVUtil.createTSVParser(
                new InputStreamReader(localities, StandardCharsets.UTF_8)
        );

        Map<Integer, LatLng> localityMap = new TreeMap<>();

        String[] line;

        while ((line = tsvParser.getLine()) != null) {
            if (line.length >= 3) {
                Integer localeNum = Integer.parseInt(line[0]);
                double latitude = Double.parseDouble(line[1]);
                double longitude = Double.parseDouble(line[2]);
                localityMap.put(localeNum, new LatLng(latitude, longitude));
            }
        }
        return localityMap;
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            Map<Integer, LatLng> localeMap = importShapes2();
            try (InputStream inputStream = getDataset().retrieve(URI.create("links"))) {
                importLinks(inputStream
                        , getInteractionListener()
                        , localeMap);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getDataset().getArchiveURI().toString() + "]", e);
        }
    }

    protected void importLinks(InputStream is, InteractionListener interactionListener, Map<Integer, LatLng> localeMap) throws IOException, StudyImporterException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createExcelCSVParse(is));
        while (parser.getLine() != null) {
            Map<String, String> e = importLink(parser, localeMap);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }

    protected Map<String, String> importLink(LabeledCSVParser parser, Map<Integer, LatLng> localeMap) throws IOException, StudyImporterException {
        TreeMap<String, String> link = new TreeMap<String, String>();

        link.put(STUDY_SOURCE_CITATION, getSourceCitationLastAccessed());

        String predNum = StringUtils.trim(parser.getValueByLabel("PredatorSciNameTSN"));
        if (StringUtils.isNotBlank(predNum)) {
            link.put(SOURCE_TAXON_ID, TaxonomyProvider.ITIS.getIdPrefix() + predNum);
        }

        String predName = StringUtils.trim(parser.getValueByLabel("PredatorSciName"));
        if (StringUtils.isNotBlank(predName)) {
            link.put(SOURCE_TAXON_NAME, predName);
        }

        String preyNum = StringUtils.trim(parser.getValueByLabel("PreySciNameTSN"));
        if (StringUtils.isNotBlank(preyNum)) {
            link.put(TARGET_TAXON_ID, TaxonomyProvider.ITIS.getIdPrefix() + preyNum);
        }

        String preyName = StringUtils.trim(parser.getValueByLabel("PreySciName"));
        if (StringUtils.isNotBlank(preyName)) {
            link.put(TARGET_TAXON_NAME, preyName);
        }

        String[] citeFields = {"CiteAuth", "CiteYear", "CiteTitle", "CiteSource", "CiteVolume", "CitePages"};
        List<String> citeValues = new ArrayList<String>();
        for (String citeField : citeFields) {
            String value = StringUtils.trim(parser.getValueByLabel(citeField));
            if (StringUtils.isNotBlank(value)) {
                String prefix;
                if ("CiteVolume".equals(citeField)) {
                    prefix = "v.";
                } else if ("CitePages".equals(citeField)) {
                    prefix = "pp.";
                } else {
                    prefix = "";
                }
                citeValues.add(prefix + value);
            }
        }
        String referenceCitation = StringUtils.join(citeValues, ". ");
        link.put(REFERENCE_ID, (getSourceDOI() == null ? "" : getSourceDOI().toString()) + '/' + MD5.getHashString(referenceCitation));
        link.put(REFERENCE_CITATION, referenceCitation);
        link.put(INTERACTION_TYPE_NAME, "preysOn");
        link.put(INTERACTION_TYPE_ID, "RO:0002439");
        link.put(LOCALITY_NAME, StringUtils.trim(parser.getValueByLabel("LocatName")));
        String locatNum = StringUtils.trim(parser.getValueByLabel("LocatNum"));
        if (StringUtils.isNotBlank(locatNum)) {
            try {
                LatLng latLng = localeMap.get(Integer.parseInt(locatNum));
                if (latLng != null) {
                    link.put(DECIMAL_LATITUDE, Double.toString(latLng.getLat()));
                    link.put(DECIMAL_LONGITUDE, Double.toString(latLng.getLng()));
                }
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("found invalid LocalNum [" + locatNum + "] " + parser.lastLineNumber(), ex);
            }
        }
        return link;
    }

}
