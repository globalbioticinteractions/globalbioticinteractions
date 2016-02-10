package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;

public class StudyImporterForPlanque extends BaseStudyImporter {
    private final static Log LOG = LogFactory.getLog(StudyImporterForPlanque.class);

    private String links;
    private String references;
    private String referencesForLinks;

    public StudyImporterForPlanque(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
        setSourceCitation("Benjamin Planque, Raul Primicerio, Kathrine Michalsen, Michaela Aschan, Grégoire Certain, Padmini Dalpadado, Harald Gjøsæater, Cecilie Hansen, Edda Johannesen, Lis Lindal Jørgensen, Ina Kolsum, Susanne Kortsch, Lise-Marie Leclerc, Lena Omli, Mette Skern-Mauritzen, and Magnus Wiedmann 2014. Who eats whom in the Barents Sea: a food web topology from plankton to whales. Ecology 95:1430–1430. http://dx.doi.org/10.1890/13-1062.1");
        setLinks("http://www.esapubs.org/archive/ecol/E095/124/revised/PairwiseList.txt");
        setReferences("http://www.esapubs.org/archive/ecol/E095/124/revised/References.txt");
        setReferencesForLinks("http://www.esapubs.org/archive/ecol/E095/124/revised/PairWise2References.txt");
    }

    protected static String normalizeName(String taxonName) {
        taxonName = taxonName.replace("_INDET", "");
        return replace(capitalize(lowerCase(taxonName)), "_", " ");
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = parserFactory.createParser(getLinks(), CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + getLinks() + "]", e);
        }
        dataParser.changeDelimiter('\t');

        Map<String, String> authorYearToFullReference = ReferenceUtil.buildRefMap(parserFactory, getReferences(), "AUTHOR_YEAR", "FULL_REFERENCE", '\t');

        Map<String, List<String>> pairwiseKeyToAuthorYears = new TreeMap<String, List<String>>();
        try {
            LabeledCSVParser referenceParser = parserFactory.createParser(getReferencesForLinks(), CharsetConstant.UTF8);
            referenceParser.changeDelimiter('\t');

            while (referenceParser.getLine() != null) {
                String pairwiseKey = referenceParser.getValueByLabel("PWKEY");
                String authorYear = referenceParser.getValueByLabel("AUTHOR_YEAR");
                if (StringUtils.isNotBlank(pairwiseKey) && StringUtils.isNotBlank(authorYear)) {
                    List<String> authorYears = pairwiseKeyToAuthorYears.get(pairwiseKey);
                    if (CollectionUtils.isEmpty(authorYears)) {
                        authorYears = new ArrayList<String>();
                    }
                    authorYears.add(authorYear);
                    pairwiseKeyToAuthorYears.put(pairwiseKey, authorYears);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + getReferencesForLinks() + "]", e);
        }

        Map<String, List<String>> pairwiseKeyToFullCitation = new TreeMap<String, List<String>>();
        for (String pairwiseKey : pairwiseKeyToAuthorYears.keySet()) {
            List<String> authorYearList = pairwiseKeyToAuthorYears.get(pairwiseKey);
            if (CollectionUtils.isEmpty(authorYearList)) {
                throw new StudyImporterException("found no AUTHOR_YEAR for PWKEY: [" + pairwiseKey + "]");
            }
            List<String> references = new ArrayList<String>();
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

        try {
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, pairwiseKeyToFullCitation);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
        return null;
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
            Study localStudy = nodeFactory.getOrCreateStudy(studyId, getSourceCitation(), ExternalIdUtil.toCitation(null, longReference, null));

            String predatorName = parser.getValueByLabel("PREDATOR");
            if (StringUtils.isBlank(predatorName)) {
                getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
            } else {
                addInteractionForPredator(parser, localStudy, predatorName);
            }
        }

    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = nodeFactory.createSpecimen(localStudy, normalizeName(predatorName));
        // from http://www.geonames.org/630674/barents-sea.html
        LocationNode location = nodeFactory.getOrCreateLocation(74.0, 36.0, null);
        predator.caughtIn(location);

        String preyName = parser.getValueByLabel("PREY");
        if (StringUtils.isBlank(preyName)) {
            getLogger().warn(localStudy, "found empty prey name on line [" + parser.lastLineNumber() + "]");
        } else {
            Specimen prey = nodeFactory.createSpecimen(localStudy, normalizeName(preyName));
            prey.caughtIn(location);
            predator.ate(prey);
        }
    }

    public void setLinks(String links) {
        this.links = links;
    }

    public String getLinks() {
        return links;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public String getReferences() {
        return references;
    }

    public void setReferencesForLinks(String referencesForLinks) {
        this.referencesForLinks = referencesForLinks;
    }

    public String getReferencesForLinks() {
        return referencesForLinks;
    }
}
