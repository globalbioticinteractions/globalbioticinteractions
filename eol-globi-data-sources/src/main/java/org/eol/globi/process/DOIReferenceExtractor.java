package org.eol.globi.process;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.ImportLogger;
import org.eol.globi.data.StudyImporterException;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOIReferenceExtractor extends InteractionProcessorAbstract {
    private static final Logger LOG = LoggerFactory.getLogger(DOIReferenceExtractor.class);

    private static final Pattern PATTERN_DOI = Pattern.compile(".*(doi:[ ]*)(10[.])(?<doiRegistrant>[^/]+)/(?<doiSuffix>[^ ]+).*");
    private static final Pattern PATTERN_DOI_URI = Pattern.compile(".*(?<doiUrl>http[s]{0,1}://(dx[.]){0,1}(doi.org/)(10[.])(?<doiRegistrant>[^/]+)/(?<doiSuffix>[^ ]+)).*");

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

                DOI doi = extractDoi(referenceCitation);

                if (doi == null) {
                    doi = extractDoiByURLPattern(referenceCitation);
                }

                if (doi != null) {
                    emittingInteraction = new TreeMap<>(interaction);
                    emittingInteraction.put(DatasetImporterForTSV.REFERENCE_DOI, doi.toString());
                }

            }
        }
        emit(emittingInteraction);
    }

    private static DOI extractDoi(String referenceCitation) {
        DOI doi = null;
        Matcher matcher = PATTERN_DOI.matcher(referenceCitation);
        if (matcher.matches()) {
            String registrant = matcher.group("doiRegistrant");
            String suffix = StringUtils.trim(matcher.group("doiSuffix"));
            suffix = stripTrailingPeriod(suffix);

            try {
                doi = new DOI(registrant, suffix);
            } catch (IllegalArgumentException ex) {
                // ignore
            }
        }
        return doi;
    }

    private static DOI extractDoiByURLPattern(String textToMatch) {
        DOI doi = null;
        Matcher urlMatcher = PATTERN_DOI_URI.matcher(textToMatch);
        if (urlMatcher.matches()) {
            String doiUrl1 = urlMatcher.group("doiUrl");
            doiUrl1 = stripTrailingPeriod(doiUrl1);
            String[] parts = StringUtils.splitByWholeSeparator(doiUrl1, "//doi.org/10.");
            if (parts.length > 1) {
                try {
                    URI escapedDoiURL = new URI("https", "doi.org", "/10." + parts[1], null);
                    doi = DOI.create(escapedDoiURL);
                } catch (URISyntaxException | MalformedDOIException e) {
                    // silently skip malformed doi urls
                    LOG.warn("unexpected issues handling DOI url [" + doiUrl1 + "]", e);
                }
            }
        }
        return doi;
    }

    private static String stripTrailingPeriod(String doiUrl1) {
        doiUrl1 = StringUtils.endsWith(doiUrl1, ".")
                ? StringUtils.substring(doiUrl1, 0, doiUrl1.length() - 1)
                : doiUrl1;
        return doiUrl1;
    }

}
