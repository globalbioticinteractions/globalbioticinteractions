package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class DatasetImporterForWood extends DatasetImporterWithListener {

    public DatasetImporterForWood(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try (InputStream resource = getDataset().retrieve(URI.create("links"))) {
            importLinks(resource, getInteractionListener(), getFilter());
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }
    }

    public void importLinks(InputStream inputStream, InteractionListener interactionListener, ImportFilter filter) throws IOException, StudyImporterException {
        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(CSVTSVUtil.createExcelCSVParse(inputStream));

        while ((filter == null || filter.shouldImportRecord((long) parser.lastLineNumber())) && parser.getLine() != null) {
            Map<String, String> e = importLink(parser);
            if (e != null) {
                interactionListener.on(e);
            }
        }
    }


    private Map<String, String> importLink(LabeledCSVParser parser) {
        Map<String, String> link = new TreeMap<String, String>();
        addTSN(parser, link, "PredTSN", TaxonUtil.SOURCE_TAXON_ID);
        link.put(TaxonUtil.SOURCE_TAXON_NAME, parser.getValueByLabel("PredName"));
        addTSN(parser, link, "PreyTSN", TaxonUtil.TARGET_TAXON_ID);
        link.put(TaxonUtil.TARGET_TAXON_NAME, parser.getValueByLabel("PreyName"));
        link.put(DatasetImporterForTSV.DATASET_CITATION, getSourceCitationLastAccessed());
        link.put(DatasetImporterForTSV.REFERENCE_CITATION, getSourceCitation());
        link.put(DatasetImporterForTSV.REFERENCE_ID, getSourceDOI().toPrintableDOI());
        link.put(DatasetImporterForTSV.REFERENCE_DOI, getSourceDOI().toString());
        link.put(DatasetImporterForTSV.REFERENCE_URL, getSourceDOI().toURI().toString());
        Term locality = DatasetImporterNodesAndLinks.localityForDataset(getDataset());
        if (locality != null) {
            link.put(DatasetImporterForTSV.LOCALITY_NAME, locality.getName());
            link.put(DatasetImporterForTSV.LOCALITY_ID, locality.getId());
        }
        LatLng latLng = DatasetImporterNodesAndLinks.locationForDataset(getDataset());
        if (latLng != null) {
            link.put(DatasetImporterForTSV.DECIMAL_LATITUDE, Double.toString(latLng.getLat()));
            link.put(DatasetImporterForTSV.DECIMAL_LONGITUDE, Double.toString(latLng.getLng()));
        }
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, "preysOn");
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "RO:0002439");
        return link;
    }

    private static void addTSN(LabeledCSVParser parser, Map<String, String> link, String tsn, String tsnLabel) {
        String tsnValue = parser.getValueByLabel(tsn);
        if (!StringUtils.startsWith(tsnValue, "san")) {
            link.put(tsnLabel, "ITIS:" + tsnValue);
        }
    }

}
