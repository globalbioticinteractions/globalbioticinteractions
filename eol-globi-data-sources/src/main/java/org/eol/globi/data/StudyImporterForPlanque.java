package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import com.Ostermiller.util.MD5;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
        refMap.put("Bjelland et al. 2000", "Bjelland, O., Bergstad, O.A., Skjæraasen, J.E. and Meland, K. 2000. Trophic ecology of deep-water fishes associated with the continental slope of the eastern Norwegian Sea. Sarsia, 85: 101-117.");
        refMap.put("Burgos & Mehl 1987", "Burgos, G., and Mehl, S. 1987. Diet overlap between North-east Arctic cod and haddock in the southern part of the Barents Sea in 1984–1986. ICES CM, 1987/G: 50: 22pp.");
        refMap.put("Dolgov et al. 2010", "Dolgov, A.V., Johannesen, E., Heino, M. and Olsen, E. 2010. Trophic ecology of blue whiting in the Barents Sea. ICES Journal of Marine Science, 67: 483-493.");
        refMap.put("Dolgov 2005", "Dolgov, A.V. 2005. Feeding and Food Consumption by the Barents Sea Skates. Journal of Northwest  Atlantic Fishery Science, 37: 495-503.");
        refMap.put("Finley & Evans 1983", "Finley K. J., Evans C. R. 1983. Summer diet of the bearded seal (Erignathus barbatus) in the Canadian High Arctic. Arctic");
        refMap.put("Gjertz & Wiig 1992", "Gjertz, I., Wiig Ø. 1992. Feeding of walrus Odobenus rosmarus in Svalbard. In Polar record.");
        refMap.put("Gundersen 1996", "Gundersen, A.C. 1996. Diettundersøkelse hos blålange, lange og brosme i storegga. Møreforskning.");
        refMap.put("Santos & Pierce 2003", "Santos M. B., Pierce G. J. 2003. The diet of harbour porpoise (Phocena phocena) in the northeast Atlantic. Oceanography and marine Biology: an Annual Review. 41: 355-390");
        refMap.put("Woll & Gundersen 2004", "Woll A. K., Gundersen A. C. 2004. Diet composition and intra-specific competition of young Greenland halibut around southern Greenalnd. Journal of Sea Reserach. 51: 243-249");
        refMap.put("Gonzáles et al. 2006", "Gonzales, C., Roman, E., Paz, X. and Ceballos, E. 2006. Feeding habits and diet overlap of skates (Amblyraja radiata, A. hyperborea, Bathyraja spinicauda, Malacoraja senta and Rajella fyllae) in the North Atlantic. Northwest Atlantic Fisheries Organization: 17.");
        refMap.put("IMR-PINRO 1994", "Kortsch pers. comm.");
        refMap.put("Falk-Petersen et al.1987", "Kortsch pers. comm.");
        refMap.put("Svalbard", "Kortsch pers. comm.");
        refMap.put("Cochrane pers. comm.", "Cochrane pers. comm.");

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
        addCorrection(refMap, "Gonzales et al. 2005", "Gonzáles et al. 2006");
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
        addCorrection(refMap, "Dolgov 2006", "Dolgov 2005");
        addCorrection(refMap, "Bjelland et al. 2010", "Bjelland et al. 2000");
        addCorrection(refMap, "Mehl 1999", "Mehl 1991");
        addCorrection(refMap, "Sazepin & Petrova 1934-1938", "Satsepin & Petrova 1934-1938");
        addCorrection(refMap, "Hinz et al. 2005", "Hinz et al., 2006");
        addCorrection(refMap, "Valtysson 1996", "Valtysson, 1995");
        addCorrection(refMap, "Valtysson 1997", "Valtysson, 1995");
        addCorrection(refMap, "Prokhorov 1967", "Prokhorov, 1965");
        addCorrection(refMap, "Dolgov 2010", "Dolgov et al. 2010");
        addCorrection(refMap, "Gjertz & Wiig 1995", "Gjertz & Wiig 1992");
        addCorrection(refMap, "Plyuscheva et al. 2010", "Plyuscheva et al. 2004");
        addCorrection(refMap, "Dolgov 2010", "Dolgov at al. 2010");
        addCorrection(refMap, "Woll & Gundersen 2003", "Woll & Gundersen 2004");
        addCorrection(refMap, "Falk-Petersen et al.1987", "Kortsch pers. comm.");
        addCorrection(refMap, "González et al. 2000", "González et al. 1999");
        addCorrection(refMap, "Dolgov and Drevetnyak 1993", "Dolgov and Drevetnyak 1993");
        addCorrection(refMap, "Burgos &  Mehl 1987", "Burgos & Mehl 1987");
    }

    private void addCorrection(Map<String, String> refMap, String original, String corrected) {
        refMap.put(original, refMap.get(corrected));
    }

    private void importLine(LabeledCSVParser parser, Map<String, String> refMap) throws StudyImporterException {
        Study localStudy = null;
        try {
            String shortReference = StringUtils.trim(parser.getValueByLabel("REFERENCE"));
            if (StringUtils.isNotBlank(shortReference)) {
                shortReference = StringUtils.replace(shortReference, "Macdonald, 1927", "Macdonald 1927");
                shortReference = StringUtils.replace(shortReference, "Vesin, Leggett et al. 1981", "Vesin et al. 1981");
                String[] refs = StringUtils.split(shortReference, ",;:");
                if (refs != null) {
                    for (String ref : refs) {
                        localStudy = importInteraction(parser, refMap, StringUtils.trim(ref));
                    }
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
        Study study = null;
        Collection<String> ignoreReferences = new ArrayList<String>() {{
            add("Fall 2011");
            add("Coyle et al. 1994");
            add("Casanova et al 2012");
        }};
        if (!ignoreReferences.contains(shortReference)) {
            study = importLink(parser, refMap, shortReference);
        }
        return study;
    }

    private Study importLink(LabeledCSVParser parser, Map<String, String> refMap, String shortReference) throws NodeFactoryException, StudyImporterException {
        String msg = null;
        String longReference;
        Study study;
        if (shortReference == null || !refMap.containsKey(shortReference)) {
            msg = "no full ref for [" + shortReference + "] on line [" + parser.lastLineNumber() + "], using short instead";
            longReference = shortReference;
        } else {
            longReference = refMap.get(shortReference);
        }

        String studyId = "PLANQUE-" + (longReference == null
                ? shortReference
                : (StringUtils.abbreviate(longReference, 20) + MD5.getHashString(longReference)));
        Study localStudy = nodeFactory.getOrCreateStudy(studyId, SOURCE, ExternalIdUtil.toCitation(null, longReference, null));
        if (StringUtils.isNotBlank(msg)) {
            getLogger().warn(localStudy, msg);
        }

        String predatorName = parser.getValueByLabel("PREDATOR");
        if (StringUtils.isBlank(predatorName)) {
            getLogger().warn(localStudy, "found empty predator name on line [" + parser.lastLineNumber() + "]");
        } else {
            addInteractionForPredator(parser, localStudy, predatorName);
        }
        study = localStudy;
        return study;
    }

    private void addInteractionForPredator(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {
        Specimen predator = nodeFactory.createSpecimen(localStudy, normalizeName(predatorName));
        // from http://www.geonames.org/630674/barents-sea.html
        Location location = nodeFactory.getOrCreateLocation(74.0, 36.0, null);
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

}
