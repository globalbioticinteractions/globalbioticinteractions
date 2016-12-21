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

public class StudyImporterForGray extends BaseStudyImporter {

    private String linksURL;

    private LatLng location;
    private Term locality;

    public StudyImporterForGray(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        setSourceCitation("Gray C, Ma A, Perkins D, Hudson L, Figueroa D, Woodward G (2015). Database of trophic interactions. Zenodo. http://dx.doi.org/10.5281/zenodo.13751");
        setSourceDOI("http://dx.doi.org/10.5281/zenodo.13751");
        setLinksURL("https://zenodo.org/record/13751/files/trophic.links.2014-11-10.csv");
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            importLinks(ResourceUtil.asInputStream(getLinksURL(), null), new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger()), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException("failed to findNamespaces: [" + getLinksURL() + "]");
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
        link.put(StudyImporterForTSV.SOURCE_TAXON_NAME, parser.getValueByLabel("consumer"));
        link.put(StudyImporterForTSV.SOURCE_LIFE_STAGE, nonNAValueOrNull(parser.getValueByLabel("consumer.lifestage")));
        link.put(StudyImporterForTSV.TARGET_TAXON_NAME, parser.getValueByLabel("resource"));
        link.put(StudyImporterForTSV.TARGET_LIFE_STAGE, nonNAValueOrNull(parser.getValueByLabel("resource.lifestage")));
        link.put(StudyImporterForTSV.STUDY_SOURCE_CITATION, getSourceCitation() + " . " + ReferenceUtil.createLastAccessedString(getLinksURL()));
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

    public void setLinksURL(String linksURL) {
        this.linksURL = linksURL;
    }

    public String getLinksURL() {
        return linksURL;
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
