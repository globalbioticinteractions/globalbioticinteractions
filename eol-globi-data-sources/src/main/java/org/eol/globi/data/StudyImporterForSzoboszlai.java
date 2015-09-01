package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.StudyImporterForTSV.*;

public class StudyImporterForSzoboszlai extends BaseStudyImporter {
    public static final String SOURCE_URL = "http://datadryad.org/bitstream/handle/10255/dryad.94536/CCPDDlinkdata_v1.csv";
    private static final String SOURCE_DOI = "doi:10.5061/dryad.nv5d2";
    private static final String STUDY_CITATION = "Szoboszlai AI, Thayer JA, Wood SA, Sydeman WJ, Koehn LE (2015) Forage species in predator diets: synthesis of data from the California Current. Ecological Informatics 29(1): 45-56. http://dx.doi.org/10.1016/j.ecoinf.2015.07.003";
    private static final String SOURCE_CITATION = "Szoboszlai AI, Thayer JA, Wood SA, Sydeman WJ, Koehn LE (2015) Data from: Forage species in predator diets: synthesis of data from the California Current. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.nv5d2 "
            + STUDY_CITATION + " " + ReferenceUtil.createLastAccessedString(SOURCE_URL);

    public StudyImporterForSzoboszlai(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            importLinks(ResourceUtil.asInputStream(SOURCE_URL, null), new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger()));
        } catch (IOException e) {
            throw new StudyImporterException("failed to find: [" + SOURCE_URL + "]");
        }
        return null;
    }

    protected void importLinks(InputStream is, InteractionListener interactionListener) throws IOException, StudyImporterException {
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(is));
        while (parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.newLink(e);
            }
        }
    }

    protected Map<String, String> importLink(LabeledCSVParser parser) throws IOException {
        TreeMap<String, String> link = new TreeMap<String, String>();

        link.put(STUDY_SOURCE_CITATION, SOURCE_CITATION);

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

        String[] citeFields = {"CiteAuth","CiteYear","CiteTitle","CiteSource","CiteVolume","CitePages"};
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
        link.put(REFERENCE_ID, SOURCE_DOI + '/' +  MD5.getHashString(referenceCitation));
        link.put(REFERENCE_CITATION, referenceCitation);
        link.put(INTERACTION_TYPE_NAME, "preysOn");
        link.put(INTERACTION_TYPE_ID, "RO:0002439");
        link.put(LOCALITY_NAME, StringUtils.trim(parser.getValueByLabel("LocatName")));

        return link;
    }
}
