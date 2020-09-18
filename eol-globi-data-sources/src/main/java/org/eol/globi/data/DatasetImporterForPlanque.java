package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;

public class DatasetImporterForPlanque extends NodeBasedImporter {
    private final static Log LOG = LogFactory.getLog(DatasetImporterForPlanque.class);

    public DatasetImporterForPlanque(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static String normalizeName(String taxonName) {
        taxonName = taxonName.replace("_INDET", "");
        return replace(capitalize(lowerCase(taxonName)), "_", " ");
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser = getParser(URI.create("links"));
        Map<String, List<String>> pairwiseKeyToAuthorYears = new TreeMap<String, List<String>>();
        try {
            LabeledCSVParser parser = getParser(getReferences());
            Map<String, String> authorYearToFullReference = ReferenceUtil.buildRefMap(parser, getReferences(),"AUTHOR_YEAR", "FULL_REFERENCE");
            LabeledCSVParser referenceParser = getParser(getReferencesForLinks());

            while (referenceParser.getLine() != null) {
                String pairwiseKey = referenceParser.getValueByLabel("PWKEY");
                String authorYear = referenceParser.getValueByLabel("AUTHOR_YEAR");
                if (StringUtils.isNotBlank(pairwiseKey) && StringUtils.isNotBlank(authorYear)) {
                    List<String> authorYears = pairwiseKeyToAuthorYears.get(pairwiseKey);
                    if (CollectionUtils.isEmpty(authorYears)) {
                        authorYears = new ArrayList<>();
                    }
                    authorYears.add(authorYear);
                    pairwiseKeyToAuthorYears.put(pairwiseKey, authorYears);
                }
            }

        Map<String, List<String>> pairwiseKeyToFullCitation = new TreeMap<>();
        for (String pairwiseKey : pairwiseKeyToAuthorYears.keySet()) {
            List<String> authorYearList = pairwiseKeyToAuthorYears.get(pairwiseKey);
            if (CollectionUtils.isEmpty(authorYearList)) {
                throw new StudyImporterException("found no AUTHOR_YEAR for PWKEY: [" + pairwiseKey + "]");
            }
            List<String> references = new ArrayList<>();
            for (String authorYear : authorYearList) {
                String reference = authorYearToFullReference.get(authorYear);
                if (StringUtils.isBlank(reference)) {
                    throw new StudyImporterException("found no FULL_CITATION for PWKEY: [" + pairwiseKey + "] and AUTHOR_YEAR [" + pairwiseKey + "]");
                } else {
                    references.add(reference);
                }
            }

            pairwiseKeyToFullCitation.put(pairwiseKey, references);
        }

            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, pairwiseKeyToFullCitation);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
    }

    private LabeledCSVParser getParser(URI links) throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = ParserFactoryForDataset.getLabeledCSVParser(getDataset().retrieve(links), CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read links", e);
        }
        dataParser.changeDelimiter('\t');
        return dataParser;
    }

    private void importLine(LabeledCSVParser parser, Map<String, List<String>> pairwiseKeyToFullCitation) throws StudyImporterException {
        Study localStudy = null;
        try {
            importLink(parser, pairwiseKeyToFullCitation);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            String message = "skipping record, found malformed field at line [" + parser.lastLineNumber() + "]: ";
            getLogger().warn(localStudy, message + e.getMessage());
        }
    }

    private void importLink(LabeledCSVParser parser, Map<String, List<String>> pairWiseKeyToFullCitation) throws NodeFactoryException, StudyImporterException {
        final String linkKey = parser.getValueByLabel("PWKEY");
        List<String> longReferences = pairWiseKeyToFullCitation.get(linkKey);
        if (CollectionUtils.isEmpty(longReferences)) {
            LOG.debug("no reference found for [" + linkKey + "] on line [" + parser.lastLineNumber() + "], using source citation instead");
            longReferences = new ArrayList<String>() {
                {
                    add(getSourceCitation());
                }
            };
        }

        for (String longReference : longReferences) {
            String studyId = "PLANQUE-" + StringUtils.abbreviate(longReference, 20) + MD5.getHashString(longReference);
            Study localStudy = getNodeFactory().getOrCreateStudy(new StudyImpl(studyId, null, ExternalIdUtil.toCitation(null, longReference, null)));

            String predatorName = parser.getValueByLabel("PREDATOR");
            if (StringUtils.isBlank(predatorName)) {
                getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
            } else {
                addInteractionForPredator(parser, localStudy, predatorName);
            }
        }

    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = getNodeFactory().createSpecimen(localStudy, new TaxonImpl(normalizeName(predatorName), null));
        // from http://www.geonames.org/630674/barents-sea.html
        Location location = getNodeFactory().getOrCreateLocation(new LocationImpl(74.0, 36.0, null, null));
        predator.caughtIn(location);

        String preyName = parser.getValueByLabel("PREY");
        if (StringUtils.isBlank(preyName)) {
            getLogger().warn(localStudy, "found empty prey name on line [" + parser.lastLineNumber() + "]");
        } else {
            Specimen prey = getNodeFactory().createSpecimen(localStudy, new TaxonImpl(normalizeName(preyName), null));
            prey.caughtIn(location);
            predator.ate(prey);
        }
    }

    public URI getReferences() {
        return URI.create("references");
    }

    public URI getReferencesForLinks() {
        return URI.create("referencesForLinks");
    }
}
