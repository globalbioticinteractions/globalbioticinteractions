package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.service.Term;
import org.eol.globi.service.UberonLookupService;

import java.io.IOException;
import java.util.List;

public class StudyImporterForBarnes extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBarnes.class);

    private TermLookupService termService = new UberonLookupService();

    public StudyImporterForBarnes(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser parser;
        try {
            parser = parserFactory.createParser("barnes/Predator_and_prey_body_sizes_in_marine_food_webs_vsn3.tsv", CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        }
        parser.changeDelimiter('\t');

        Study study = nodeFactory.getOrCreateStudy("Barnes 2008", "C. Barnes et al.", "Centre for Environment, Fisheries and Aquaculture Science, Lowestoft, Suffolk, NR33 0HT  UK", "", "<a href=\"http://www.esapubs.org/Archive/ecol/E089/051/\">Predator and prey body sizes in marine food webs.</a>", "2008", StudyImporterForGoMexSI.GOMEXSI_URL);
        try {
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) parser.getLastLineNumber())) {
                    importLine(parser, study);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + parser.lastLineNumber() + "]", e);
        }
        return study;
    }

    private void importLine(LabeledCSVParser parser, Study study) throws StudyImporterException {
        try {
            Specimen predator = nodeFactory.createSpecimen(parser.getValueByLabel("Predator"));
            addLifeStage(parser, predator);

            Double latitude = LocationUtil.parseDegrees(parser.getValueByLabel("Latitude"));
            Double longitude = LocationUtil.parseDegrees(parser.getValueByLabel("Longitude"));
            String depth = parser.getValueByLabel("Depth");
            Double altitudeInMeters = -1.0 * Double.parseDouble(depth);
            Location location = nodeFactory.getOrCreateLocation(latitude, longitude, altitudeInMeters);
            predator.caughtIn(location);

            Specimen prey = nodeFactory.createSpecimen(parser.getValueByLabel("Prey"));

            predator.ate(prey);

            study.collected(predator);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            LOG.warn("skipping record, found malformed field at line [" + parser.lastLineNumber() + "]", e);
        }
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen predator) throws StudyImporterException {
        String lifeStageString = parser.getValueByLabel("Predator lifestage");
        try {
            List<Term> terms = termService.lookupTermByName(lifeStageString);
            if (terms.size() == 0) {
                throw new StudyImporterException("unsupported life stage [" + lifeStageString + "] on line [" + parser.getLastLineNumber() + "]");
            }
            predator.setLifeStage(terms);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException(("failed to map life stage [" + lifeStageString + "]"));
        }
    }

}
