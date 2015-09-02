package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

public class StudyImporterForWood extends BaseStudyImporter {

    private String archiveURL;

    private LatLng location;
    private Term locality;

    public StudyImporterForWood(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        setSourceCitation("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.g1qr6");
        setSourceDOI("http://dx.doi.org/10.1002/ece3.1640");
        setArchiveURL("http://datadryad.org/bitstream/handle/10255/dryad.93018/WoodEtal_Append1_v2.csv");
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            importLinks(ResourceUtil.asInputStream(getArchiveURL(), null), new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger()), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException("failed to find: [" + getArchiveURL() + "]");
        }
        return null;
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
        link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getSourceCitation() + ReferenceUtil.createLastAccessedString(getArchiveURL()));
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

    public void setArchiveURL(String archiveURL) {
        this.archiveURL = archiveURL;
    }

    public String getArchiveURL() {
        return archiveURL;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setLocality(Term locality) {
        this.locality = locality;
    }

    public LatLng getLocation() {
        return location;
    }

    public Term getLocality() {
        return locality;
    }

}
