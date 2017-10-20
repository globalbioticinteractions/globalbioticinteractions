package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

interface DietMatrixListener {
    void onMatrix(String matrix) throws StudyImporterException;
}

public class StudyImporterForGlobalWebDb extends BaseStudyImporter {

    public StudyImporterForGlobalWebDb(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        InteractionListenerImpl interactionListenerImpl = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
        importDietMatrices("https://www.globalwebdb.com/Service/DownloadArchive", dietMatrixString -> {
            try {
                parseDietMatrix(interactionListenerImpl, dietMatrixString, getSourceCitation());
            } catch (IOException e) {
                throw new StudyImporterException(e);
            }
        });
    }

    static void parseDietMatrix(InteractionListener listener, String dietMatrixWithCitation, String sourceCitation) throws IOException, StudyImporterException {
        String[] rows = dietMatrixWithCitation.split("\r\n");
        if (rows.length > 0) {
            String citation = rows[0].replaceAll("^\"", "")
                    .replaceAll("\",*$", "");

            List<String> matrixRows = Arrays.asList(rows).subList(1, rows.length);
            String matrix = org.apache.commons.lang.StringUtils.join(matrixRows, "\n");

            LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(IOUtils.toInputStream(matrix));
            String[] headerColumns = parser.getLabels();
            if (headerColumns.length > 1) {
                String[] split1 = headerColumns[0].split("-");
                String habitat = split1[0];
                List<String> localityList = Arrays.asList(split1).subList(1, split1.length);
                String locality = localityList.stream().map(String::trim).collect(Collectors.joining(", "));

                Map<String, String> props = new TreeMap<String, String>() {{
                    put(StudyImporterForTSV.HABITAT_NAME, org.apache.commons.lang.StringUtils.trim(habitat));
                    put(StudyImporterForTSV.LOCALITY_NAME, org.apache.commons.lang.StringUtils.trim(locality));
                    put(StudyImporterForTSV.INTERACTION_TYPE_NAME, InteractType.ATE.getLabel());
                    put(StudyImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
                    put(StudyImporterForTSV.REFERENCE_ID, MD5.getHashString(citation));
                    put(StudyImporterForTSV.REFERENCE_CITATION, citation);
                    put(StudyImporterForTSV.STUDY_SOURCE_CITATION, sourceCitation);
                }};

                List<String> sourceTaxa = Arrays.asList(headerColumns).subList(1, headerColumns.length);
                while (parser.getLine() != null) {
                    for (String sourceTaxon : sourceTaxa) {
                        String value = parser.getValueByLabel(sourceTaxon);
                        String targetTaxon = parser.getValueByLabel(headerColumns[0]);
                        if (NumberUtils.isDigits(value) && Integer.parseInt(value) > 0) {
                            listener.newLink(new TreeMap<String, String>(props) {{
                                put(StudyImporterForTSV.SOURCE_TAXON_NAME, org.apache.commons.lang.StringUtils.trim(sourceTaxon));
                                put(StudyImporterForTSV.TARGET_TAXON_NAME, org.apache.commons.lang.StringUtils.trim(targetTaxon));
                            }});
                        }
                    }
                }
            }
        }
    }

    void importDietMatrices(String archiveURL, DietMatrixListener matrix) throws StudyImporterException {
        try {
            InputStream inputStream = getDataset().getResource(archiveURL);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().matches("WEB.*\\.csv$")) {
                    matrix.onMatrix(IOUtils.toString(zipInputStream));
                } else {
                    IOUtils.copy(zipInputStream, new NullOutputStream());
                }
            }
            IOUtils.closeQuietly(zipInputStream);
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }
    }
}
