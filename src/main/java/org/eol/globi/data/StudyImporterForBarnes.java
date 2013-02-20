package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;

public class StudyImporterForBarnes extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBarnes.class);


    public StudyImporterForBarnes(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser parser = null;
        try {
            parser = parserFactory.createParser("barnes/Predator_and_prey_body_sizes_in_marine_food_webs_vsn3.tsv.gz");
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource", e);
        }
        parser.changeDelimiter('\t');

        Study study = nodeFactory.getOrCreateStudy(StudyLibrary.Study.BARNES.toString(), "C. Barnes et al.", "Centre for Environment, Fisheries and Aquaculture Science, Lowestoft, Suffolk, NR33 0HT  UK", "", "<a href=\"http://www.esapubs.org/Archive/ecol/E089/051/\">Predator and prey body sizes in marine food webs.</a>");
        try {
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord((long)parser.getLastLineNumber())) {
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
            Taxon predatorTaxon = nodeFactory.getOrCreateTaxon(parser.getValueByLabel("Predator"));
            Specimen predator = nodeFactory.createSpecimen();
            predator.classifyAs(predatorTaxon);

            Double latitude = parseDegrees(parser.getValueByLabel("Latitude"));
            Double longitude = parseDegrees(parser.getValueByLabel("Longitude"));
            String depth = parser.getValueByLabel("Depth");
            Double altitudeInMeters = -1.0 * Double.parseDouble(depth);
            Location location = nodeFactory.getOrCreateLocation(latitude, longitude, altitudeInMeters);
            predator.caughtIn(location);

            Specimen prey = nodeFactory.createSpecimen();
            Taxon preyTaxon = nodeFactory.getOrCreateTaxon(parser.getValueByLabel("Prey"));
            prey.classifyAs(preyTaxon);

            predator.ate(prey);

            study.collected(predator);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            LOG.warn("skipping record, found malformed field at line [" + parser.lastLineNumber() + "]", e);
        }
    }

    public static Double parseDegrees(String latString) {
        String[] split = latString.split("º");
        if (split.length == 1) {
            split = latString.split("�");
        }
        Double degrees = 0.0;
        Double minutes = 0.0;
        Double seconds = 0.0;
        if (split.length > 1) {
            degrees = Double.parseDouble(split[0]);
            split = split[1].split("'");
            if (split.length > 1) {
                minutes = Double.parseDouble(split[0]);
                split = split[1].split("''");
                if (split.length > 1) {
                    seconds = Double.parseDouble(split[0]);
                }
            }


        }
        Double lat = degrees + minutes / 60.0 + seconds / 3600.0;
        lat = lat * (latString.endsWith("N") || latString.endsWith("E") ? 1.0 : -1.0);
        return lat;
    }
}
