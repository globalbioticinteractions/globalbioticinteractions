package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StudyImporterForBarnes extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBarnes.class);

    public static final String SOURCE = "Barnes, C. et al., 2008. PREDATOR AND PREY BODY SIZES IN MARINE FOOD WEBS. Ecology, 89(3), pp.881–881. Available at: https://doi.org/10.1890/07-1551.1 . Data provided by Carolyn Barnes. Also available at " + "http://www.esapubs.org/Archive/ecol/E089/051/" + " .";
    public static final String RESOURCE_PATH = "http://esapubs.org/archive/ecol/E089/051/Predator_and_prey_body_sizes_in_marine_food_webs_vsn4.txt";
    public static final String REFERENCE_PATH = "barnes/references.csv";

    public StudyImporterForBarnes(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = parserFactory.createParser(RESOURCE_PATH, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + RESOURCE_PATH + "]", e);
        }
        dataParser.changeDelimiter('\t');

        Map<String, String> refMap = ReferenceUtil.buildRefMap(parserFactory, REFERENCE_PATH);

        try {
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, refMap);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
    }

    private void importLine(LabeledCSVParser parser, Map<String, String> refMap) throws StudyImporterException {
        Study localStudy = null;
        try {
            String shortReference = StringUtils.trim(parser.getValueByLabel("Reference"));
            if (!refMap.containsKey(shortReference)) {
                throw new StudyImporterException("failed to find ref [" + shortReference + "] on line [" + parser.lastLineNumber() + "]");
            }
            String longReference = refMap.get(shortReference);
            localStudy = nodeFactory.getOrCreateStudy(new StudyImpl("BARNES-" + shortReference, SOURCE, null, ExternalIdUtil.toCitation(null, longReference, null)));

            String predatorName = parser.getValueByLabel("Predator");
            if (StringUtils.isBlank(predatorName)) {
                getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
            } else {
                addInteractionForPredator(parser, localStudy, predatorName);
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            String message = "skipping record, found malformed field at line [" + parser.lastLineNumber() + "]: ";
            if (localStudy != null) {
                getLogger().warn(localStudy, message + e.getMessage());
            }
        }
    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = nodeFactory.createSpecimen(localStudy, new TaxonImpl(predatorName, null));
        addLifeStage(parser, predator);

        Double latitude = LocationUtil.parseDegrees(parser.getValueByLabel("Latitude"));
        Double longitude = LocationUtil.parseDegrees(parser.getValueByLabel("Longitude"));
        String depth = parser.getValueByLabel("Depth");
        Double altitudeInMeters = -1.0 * Double.parseDouble(depth);
        Location location = nodeFactory.getOrCreateLocation(new LocationImpl(latitude, longitude, altitudeInMeters, null));
        predator.caughtIn(location);

        String preyName = parser.getValueByLabel("Prey");
        if (StringUtils.isBlank(preyName)) {
            getLogger().warn(localStudy, "found empty prey name on line [" + parser.lastLineNumber() + "]");
        } else {
            Specimen prey = nodeFactory.createSpecimen(localStudy, new TaxonImpl(preyName, null));
            prey.caughtIn(location);
            predator.ate(prey);
        }
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen predator) throws StudyImporterException {
        String lifeStageString = parser.getValueByLabel("Predator lifestage");
        try {
            List<Term> terms = nodeFactory.getTermLookupService().lookupTermByName(lifeStageString);
            if (terms.size() == 0) {
                throw new StudyImporterException("unsupported life stage [" + lifeStageString + "] on line [" + parser.getLastLineNumber() + "]");
            }
            predator.setLifeStage(terms);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException(("failed to map life stage [" + lifeStageString + "]"));
        }
    }

}
