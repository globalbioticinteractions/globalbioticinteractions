package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;

public class StudyImporterForPlanque extends BaseStudyImporter {
    public static final String SOURCE = "Benjamin Planque, Raul Primicerio, Kathrine Michalsen, Michaela Aschan, Grégoire Certain, Padmini Dalpadado, Harald Gjøsæater, Cecilie Hansen, Edda Johannesen, Lis Lindal Jørgensen, Ina Kolsum, Susanne Kortsch, Lise-Marie Leclerc, Lena Omli, Mette Skern-Mauritzen, and Magnus Wiedmann 2014. Who eats whom in the Barents Sea: a food web topology from plankton to whales. Ecology 95:1430–1430. http://dx.doi.org/10.1890/13-1062.1";
    public static final String RESOURCE_PATH = "http://www.esapubs.org/archive/ecol/E095/124/PairwiseList.txt";
    public static final String REFERENCE_PATH = "http://www.esapubs.org/archive/ecol/E095/124/References.txt";

    public StudyImporterForPlanque(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static String normalizeName(String taxonName) {
        taxonName = taxonName.replace("_INDET", "");
        return replace(capitalize(lowerCase(taxonName)), "_", " ");
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = parserFactory.createParser(RESOURCE_PATH, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + RESOURCE_PATH + "]", e);
        }
        dataParser.changeDelimiter('\t');

        Map<String, String> refMap = ReferenceUtil.buildRefMap(parserFactory, REFERENCE_PATH, "AUTHOR_YEAR", "FULL_REFERENCE", '\t');
        correctSuspectedTypos(refMap);

        try {
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, refMap);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
        return null;
    }

    protected void correctSuspectedTypos(Map<String, String> refMap) {
        addCorrection(refMap, "Marsakis & Conover 1991", "Matsakis & Conover 1991");
        addCorrection(refMap, "Moura et al. 2005", "Moura et al. 2005;");
        addCorrection(refMap, "Moura et al.2005", "Moura et al. 2005;");
        addCorrection(refMap, "Moura 2005", "Moura et al. 2005;");
        addCorrection(refMap, "Gonzales et al. 2006", "Gonzáles et al. 2006");
        addCorrection(refMap, "Gonzales 2006", "Gonzáles et al. 2006");
        addCorrection(refMap, "Thiemann et al. 2008", "Thiemann 2008");
        addCorrection(refMap, "Tonnesson & Tiselius 2005", "Tonneson & Tiselius 2005");
        addCorrection(refMap, "T_nneson & Tiselius 2005", "Tonneson & Tiselius 2005");
        addCorrection(refMap, "Bjelland 2000", "Bjelland et al. 2000");
        addCorrection(refMap, "Román et al 2004", "Román et al. 2004");
        addCorrection(refMap, "Hudson et al. 2004", "Hudson et al. 2004.");
        addCorrection(refMap, "Emson 1991", "Emson et al. 1991");
        addCorrection(refMap, "Michaelaen & Nedreaas 1998", "Michaelsen & Nedreaas 1998");
        addCorrection(refMap, "Michalsen & Nedreaas 1998", "Michaelsen & Nedreaas 1998");
        addCorrection(refMap, "Michaelaen & Nedreaas 1998", "Michaelsen & Nedreaas 1998");
        addCorrection(refMap, "Michaelsen & Nedereaas 1998", "Michaelsen & Nedreaas 1998");
        addCorrection(refMap, "Arnud & Bamber 1987", "Arnaud & Bamber 1987");
        addCorrection(refMap, "Aarefjord & Bjørge 1995", "Aarefjord et al. 1995");
        addCorrection(refMap, "Laidre et al. 2006", "Laidre et. al 2006");
        addCorrection(refMap, "Aguilar et al. 2009", "Aguilar 2009");
        addCorrection(refMap, "Barret et al. 1987", "Barrett et al. 1987");
        addCorrection(refMap, "Barret et al. 2002", "Barrett et al. 2002");
        addCorrection(refMap, "Klemestsen 1982", "Klemetsen 1982");
        addCorrection(refMap, "Bowman 2000", "Bowman et al. 2000");
        addCorrection(refMap, "Pedersen et al. 1999", "Pedersen 1999");
        addCorrection(refMap, "Gordon & Duncan1979", "Gordon & Duncan 1979");
        addCorrection(refMap, "Vatlysson 1995", "Valtysson 1995");
    }

    private void addCorrection(Map<String, String> refMap, String original, String corrected) {
        refMap.put(original, refMap.get(corrected));
    }

    private void importLine(LabeledCSVParser parser, Map<String, String> refMap) throws StudyImporterException {
        Study localStudy = null;
        try {
            String shortReference = StringUtils.trim(parser.getValueByLabel("REFERENCE"));
            if (StringUtils.isNotBlank(shortReference)) {
                shortReference = shortReference.replace("Macdonald, 1927", "Macdonald 1927");
                shortReference = shortReference.replace("Vesin, Leggett et al. 1981", "Vesin et al. 1981");
                String[] refs = StringUtils.split(shortReference, ",;:");
                for (String ref : refs) {
                    localStudy = importInteraction(parser, refMap, StringUtils.trim(ref));
                }
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

    private Study importInteraction(LabeledCSVParser parser, Map<String, String> refMap, String shortReference) throws NodeFactoryException, StudyImporterException {
        String longReference;
        String msg = null;
        if (shortReference == null || !refMap.containsKey(shortReference)) {
            msg = "no full ref for [" + shortReference + "] on line [" + parser.lastLineNumber() + "], using short instead";
            longReference = shortReference;
        } else {
            longReference = refMap.get(shortReference);
        }

        Study localStudy = nodeFactory.getOrCreateStudy("PLANGUE-" + shortReference, null, null, null, longReference, null, SOURCE);
        if (StringUtils.isNotBlank(msg)) {
            getLogger().warn(localStudy, msg);
        }

        String predatorName = parser.getValueByLabel("PREDATOR");
        if (StringUtils.isBlank(predatorName)) {
            getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
        } else {
            addInteractionForPredator(parser, localStudy, predatorName);
        }
        return localStudy;
    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = nodeFactory.createSpecimen(normalizeName(predatorName));
        // from http://www.geonames.org/630674/barents-sea.html
        Location location = nodeFactory.getOrCreateLocation(74.0, 36.0, null);
        predator.caughtIn(location);

        String preyName = parser.getValueByLabel("PREY");
        if (StringUtils.isBlank(preyName)) {
            getLogger().warn(localStudy, "found empty prey name on line [" + parser.lastLineNumber() + "]");
        } else {
            Specimen prey = nodeFactory.createSpecimen(normalizeName(preyName));
            predator.ate(prey);
        }

        localStudy.collected(predator);
    }

}
