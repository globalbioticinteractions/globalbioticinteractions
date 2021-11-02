package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.AssociatedTaxaUtil;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;
import org.globalbioticinteractions.doi.DOI;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOIReferenceExtractor extends InteractionProcessorAbstract {

    public DOIReferenceExtractor(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> emittingInteraction = interaction;

        if (!interaction.containsKey(DatasetImporterForTSV.REFERENCE_DOI)
                && !interaction.containsKey(DatasetImporterForTSV.REFERENCE_URL)
                && interaction.containsKey(DatasetImporterForTSV.REFERENCE_CITATION)) {

            String referenceCitation = interaction.get(DatasetImporterForTSV.REFERENCE_CITATION);
            if (StringUtils.isNoneBlank(referenceCitation)) {
                Matcher matcher = Pattern.compile(".*(doi:)([ ]*)(10[.])(.*)/([^ ]+)").matcher(referenceCitation);
                if (matcher.matches()) {
                    String registrant = matcher.group(4);
                    String suffix = matcher.group(5);
                    try {
                        DOI doi = new DOI(registrant, suffix);
                        emittingInteraction = new TreeMap<String, String>(interaction) {{
                            put(DatasetImporterForTSV.REFERENCE_DOI, doi.toString());
                        }};
                    } catch (IllegalArgumentException ex) {
                        // ignore
                    }
                }
            }
        }
        emit(emittingInteraction);
    }

}
